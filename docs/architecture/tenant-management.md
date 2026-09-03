# Tenant Management and Cloud Deployment Guide

This guide explains the LMS tenant-management implementation across the backend, frontend, Supabase, Cloudflare R2, and the certificate service. It is intended for developers and operators working on the `feature/tenant-control-plane` branch.

## 1. What we implemented

The LMS now uses a **database-per-tenant** architecture.

- A dedicated **control-plane** database stores the platform administrator and tenant registry.
- Each customer tenant has its own Supabase project and its own LMS data.
- A global platform administrator creates, provisions, suspends, pauses, restores, and schedules deletion of tenants.
- Tenant administrators, instructors, and students use the normal LMS application; they cannot access the platform console.
- Tenant identity is derived from the workspace hostname, not from a user-selected value in the UI.

This provides database-level data isolation: one tenant cannot query or modify another tenant's LMS records.

## 2. Current Supabase role split

| Supabase project | Role | Data it should contain |
| --- | --- | --- |
| **LMS Integration Test** | Control plane | `platform` schema: platform admins, tenant registry, and tenant audit events. The application keeps its required `lms` schema/migrations there as a technical compatibility requirement, but it is not the tenant with the retained mock LMS data. |
| **LMS** | Active tenant | Existing LMS mock data: users, courses, enrolments, assessments, and related `lms` data. It is registered in the control plane with the slug `lms`. |

The control plane does **not** use tenant course, assessment, or learner data to operate the platform. Its authoritative platform records are in the `platform` schema.

## 3. System layout

```text
platform.<base-domain>
  -> Frontend platform console
  -> LMS backend control-plane datasource
  -> control-plane Supabase project / platform schema
  -> tenant registry and lifecycle actions

<tenant-slug>.<base-domain>
  -> Normal LMS frontend
  -> LMS backend resolves the slug
  -> selected tenant Supabase project / lms schema
  -> tenant-only users, courses, enrolments, assessments, etc.

Certificate service
  -> reads the same control-plane tenant registry
  -> routes certificate work to the selected tenant database
  -> stores certificate files under Certificates/<tenantId>/ in the shared R2 bucket
```

For local development the domain suffix is `.localhost`:

- Control plane: `http://platform.localhost:3000`
- LMS tenant: `http://lms.localhost:3000`
- Another tenant named `acme`: `http://acme.localhost:3000`

For deployment set `TENANT_BASE_DOMAIN` and `VITE_TENANT_BASE_DOMAIN` to the production base domain. For example, `acme.lms.example.com` resolves the tenant slug `acme` when the base domain is `lms.example.com`.

## 4. Authentication and authorization

There are two independent authentication boundaries.

### Global platform administrator

- Stored in `platform.platform_admins` in the control-plane database.
- Authenticates only at `platform.<base-domain>`.
- Receives the `PLATFORM_ADMIN` authority.
- Can access `/platform/tenants` and the platform API.
- Creates and manages tenants, but does **not** become a tenant LMS administrator.

### Tenant LMS users

- Stored in the selected tenant's `lms.users` table.
- Authenticate through the normal `/auth/login` page on that tenant's hostname.
- Use normal LMS roles such as `ADMIN`, `INSTRUCTOR`, and `STUDENT`.
- Never receive access to the global tenant-management UI.

Do not use the root `localhost` hostname as a tenant login URL. It has no tenant slug and is intentionally treated as a platform boundary. Use the tenant subdomain instead.

## 5. Backend implementation

The control-plane schema is created by:

- `src/main/resources/db/migration/V20260901_3000__create_platform_tenant_control_plane.sql`

It creates:

- `platform.platform_admins`
- `platform.tenants`
- `platform.tenant_audit_events`

The backend tenant code lives under `com.lms.platform` and handles:

- platform-admin authentication;
- tenant registration and lifecycle audit events;
- encrypted storage of tenant database credentials;
- Supabase Management API provisioning and cloud pause/restore requests;
- data-source selection for a request tenant;
- tenant migration and initial tenant-admin creation.

The public platform-management API is protected by `PLATFORM_ADMIN`:

| Operation | Endpoint |
| --- | --- |
| List tenants | `GET /api/v1/platform/tenants` |
| Register a tenant | `POST /api/v1/platform/tenants` |
| Continue/retry provisioning | `POST /api/v1/platform/tenants/{tenantId}/provision` |
| Suspend application access | `POST /api/v1/platform/tenants/{tenantId}/suspend` |
| Pause Supabase project | `POST /api/v1/platform/tenants/{tenantId}/pause-cloud` |
| Restore Supabase project | `POST /api/v1/platform/tenants/{tenantId}/restore-cloud` |
| Schedule deletion | `POST /api/v1/platform/tenants/{tenantId}/schedule-deletion` |

### Tenant states

| State | Meaning |
| --- | --- |
| `PROVISIONING` | A tenant record exists; the cloud project and/or migrations are being prepared. |
| `ACTIVE` | The tenant database is ready and normal LMS access is available. |
| `SUSPENDED` | LMS access is blocked by the platform administrator; the cloud database still exists. |
| `CLOUD_PAUSING` | Supabase pause has been requested and the backend is waiting for confirmation. |
| `CLOUD_PAUSED` | The Supabase project is paused; tenant access is unavailable while data is retained. |
| `CLOUD_RESTORING` | Supabase restore has been requested and the backend is waiting for the project to become ready. |
| `DELETION_SCHEDULED` | Tenant is marked for a 30-day retention period before permanent deletion work. |
| `PROVISION_FAILED` | Provisioning failed; correct configuration and retry it from the console. |

The scheduled backend task reconciles cloud pause and restore status with Supabase. A temporary `CLOUD_RESTORING` state is expected immediately after a restore request.

## 6. Required backend configuration

Copy `.env.example` to `.env`. Keep `.env` private and use deployment secret management in shared environments.

The tenant-management settings are:

```dotenv
PLATFORM_ENABLED=true
TENANT_RESOLUTION_REQUIRED=true
TENANT_HEADER=X-Tenant-Slug
TENANT_BASE_DOMAIN=localhost
TENANT_CREDENTIAL_ENCRYPTION_KEY=<base64-encoded-32-byte-key>

PLATFORM_ADMIN_ENABLED=true
PLATFORM_ADMIN_EMAIL=<global-platform-admin-email>
PLATFORM_ADMIN_PASSWORD=<global-platform-admin-password>
PLATFORM_ADMIN_NAME=Platform Administrator

TENANT_PROVISIONING_PROVIDER=SUPABASE
SUPABASE_ACCESS_TOKEN=<Supabase-personal-access-token>
SUPABASE_ORGANIZATION_SLUG=<Supabase-organization-slug>
SUPABASE_TENANT_REGION=ap-southeast-1
```

Important rules:

1. `TENANT_CREDENTIAL_ENCRYPTION_KEY` encrypts passwords in the tenant registry. Losing or changing it prevents the platform from using already registered tenant databases. Store it in a secure secret manager and back it up securely.
2. `SUPABASE_ACCESS_TOKEN` is a server-side secret. Never put it in a frontend environment file or commit it.
3. The global platform admin is distinct from every tenant bootstrap admin.
4. Enable `PLATFORM_ADMIN_ENABLED` only long enough to create/update the platform-admin account on a fresh control plane; then set it to `false`.
5. Use a Supabase **session pooler** JDBC URL on port `5432` with `sslmode=require`. The transaction pooler is not suitable for Flyway advisory locks.

## 7. Supabase provisioning flow

When a global administrator submits **Add tenant**:

1. The backend validates and reserves the unique slug in `platform.tenants`.
2. The backend requests a new project using the Supabase Management API.
3. The backend polls the provider until the project is ready.
4. The backend connects to the tenant project with its generated database credentials.
5. Flyway applies LMS migrations to the tenant `lms` schema.
6. The submitted tenant administrator is created in that tenant database.
7. The tenant becomes `ACTIVE` and can sign in at its hostname.

The free Supabase plan has a limited number of active projects. Pausing a tenant preserves the database/data but releases its active-project capacity. Restoring it can take a few minutes; use the refresh button in the platform console until the status returns to `ACTIVE`.

## 8. Frontend implementation

The frontend code is in `LMS-FrontEnd/lms-frontend`.

- `src/utils/tenantHostname.js` maps a browser hostname to a tenant slug.
- `src/features/platform` contains the global platform login and tenant management console.
- `src/features/tenants` contains normal tenant organization settings.
- Platform route protection verifies both the platform token and the `platform.<base-domain>` hostname.
- Normal LMS navigation is used at tenant hostnames; the platform console is not displayed there.

Set the frontend environment variable:

```dotenv
VITE_TENANT_BASE_DOMAIN=localhost
```

For production, replace `localhost` with the production base domain and configure wildcard DNS plus TLS for `*.lms.example.com` (or the selected base domain).

## 9. Certificate service and R2

The certificate service is tenant-aware and must use the same:

- control-plane database;
- `JWT_SECRET` as LMS-BackEnd;
- `SERVICE_KEY_SECRET` as LMS-BackEnd;
- `TENANT_CREDENTIAL_ENCRYPTION_KEY` as LMS-BackEnd;
- tenant header/name resolution configuration.

Certificate objects reuse the existing Cloudflare R2 bucket but are isolated by prefix:

```text
courses/...                 Existing LMS course assets
Certificates/<tenantId>/... Tenant certificate PDFs and assets
```

The service must never write certificate files into the `courses/` prefix. R2 credentials remain server-side secrets.

## 10. Local test checklist

1. Start the LMS backend, certificate service, and frontend using their `.env` files.
2. Sign in to `http://platform.localhost:3000` as the global platform administrator.
3. Confirm the platform console lists the `lms` tenant as `ACTIVE`.
4. Open `http://lms.localhost:3000/auth/login` and sign in with a tenant LMS account.
5. Confirm that tenant admin sees the normal LMS administration pages, not the platform tenant console.
6. In the platform console, test Suspend, Pause cloud DB, Restore cloud DB, and refresh lifecycle status. Do this only against a non-production test tenant.
7. Confirm certificate creation/retrieval uses the correct tenant and writes only below `Certificates/<tenantId>/`.

## 11. Operational checklist before production

- [ ] Create a dedicated Supabase control-plane project.
- [ ] Store all backend and certificate-service secrets in the deployment secret manager.
- [ ] Configure wildcard DNS and TLS for the tenant domain.
- [ ] Set CORS to only trusted frontend origins.
- [ ] Configure a custom Cloudflare R2 domain; the public development URL is rate-limited and not intended for production.
- [ ] Set up backups and retention policies for both control-plane and tenant projects.
- [ ] Add monitoring/alerts for provisioning failures and cloud lifecycle failures.
- [ ] Implement the actual permanent-delete worker before processing deletion-scheduled tenants automatically.
- [ ] Test provisioning, tenant login, suspension, cloud pause/restore, certificate issuance, and a tenant recovery procedure in a non-production environment.

## 12. What not to do

- Do not commit `.env`, Supabase tokens, R2 keys, database passwords, JWT secrets, or encryption keys.
- Do not log tenant database passwords or initial tenant-admin passwords.
- Do not allow a tenant LMS `ADMIN` role to call `/api/v1/platform/**`.
- Do not route root `localhost` traffic to an arbitrary tenant.
- Do not delete a tenant Supabase project immediately; honour the scheduled retention window and verify backups first.

## 13. Source locations

| Area | Location |
| --- | --- |
| Control-plane migration | `LMS-BackEnd/src/main/resources/db/migration/V20260901_3000__create_platform_tenant_control_plane.sql` |
| Tenant backend | `LMS-BackEnd/src/main/java/com/lms/platform/` |
| Tenant routing backend | `LMS-BackEnd/src/main/java/com/lms/platform/runtime/` |
| Platform frontend | `LMS-FrontEnd/lms-frontend/src/features/platform/` |
| Hostname helper | `LMS-FrontEnd/lms-frontend/src/utils/tenantHostname.js` |
| Certificate tenancy | `lms-certificate-service/lms-certificate-service/src/main/java/com/lms/certificate/tenancy/` |
| Certificate R2 storage | `lms-certificate-service/lms-certificate-service/src/main/java/com/lms/certificate/storage/R2StorageService.java` |

