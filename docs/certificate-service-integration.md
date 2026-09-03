# Certificate Service Integration

> **Related repo:** `lms-certificate-service` (separate microservice)

## Overview

This document describes the minimal changes made to `LMS-BackEnd` to support
the standalone `lms-certificate-service` microservice.

The certificate service owns all certificate lifecycle logic (issuance, PDF
generation, revocation, verification). It is a fully independent Spring Boot
application with its own PostgreSQL schema (`cert`). It never touches the LMS
database directly — it fetches the data it needs from the **internal API**
described here.

---

## Architecture

```
lms-certificate-service
        │
        │  JWT (validated via JWKS public key)
        │  Internal API key (X-Service-Key header)
        ▼
LMS-BackEnd  /api/v1/internal/**
        │
        ├── GET /users/{id}          → name, email
        ├── GET /courses/{id}        → title, durationMinutes, thumbnailKey
        ├── GET /enrollments         → completedAt, enrolledAt, status
        └── GET /org-settings        → name, logoUrl, primaryColor
```

The cert service also receives an **event webhook** from LMS-BackEnd whenever
an enrollment transitions to `COMPLETED`, triggering automatic certificate
issuance.

---

## Changes Made to LMS-BackEnd

### 1. JWKS Endpoint — `JwksController`

`GET /api/v1/.well-known/jwks.json` (public, no auth required)

Exposes the HMAC signing key as a symmetric JWK so the cert service can
independently validate tokens issued by LMS-BackEnd.

> **Note:** The current implementation uses HS256 (symmetric HMAC). The JWKS
> endpoint exposes the `kid` and `alg` only — the actual secret is **never**
> returned. The cert service shares the same `JWT_SECRET` env var and uses the
> kid to confirm key identity. This is a pragmatic choice; migrating to RS256
> (asymmetric) would be the next security hardening step.

### 2. Internal API — `InternalController`

`/api/v1/internal/**` — protected by the `X-Service-Key` header.

All responses are minimal read-only projections. No write operations are
exposed on the internal API.

| Endpoint | Returns |
|---|---|
| `GET /api/v1/internal/users/{id}` | `InternalUserDto` |
| `GET /api/v1/internal/courses/{id}` | `InternalCourseDto` |
| `GET /api/v1/internal/enrollments?studentId=&courseId=` | `InternalEnrollmentDto` |
| `GET /api/v1/internal/org-settings` | `InternalOrgSettingsDto` |

### 3. Service Key Filter — `ServiceKeyAuthFilter`

A `OncePerRequestFilter` that protects `/api/v1/internal/**`.
Requests must include `X-Service-Key: <SERVICE_KEY_SECRET>` or receive 401.

### 4. Enrollment Completion Webhook — `CertificateWebhookClient`

When `EnrollmentServiceImpl.doUpdateEnrollmentStatus()` transitions an
enrollment to `COMPLETED`, it fires a non-blocking async call to the cert
service's internal webhook endpoint to trigger auto-issuance.

The webhook call is fire-and-forget: a failure is logged but does not roll
back the enrollment transaction.

### 5. New Config Properties

```yaml
lms:
  internal:
    service-key: ${SERVICE_KEY_SECRET}      # shared secret for internal API
  certificate-service:
    base-url: ${CERT_SERVICE_URL:http://localhost:8081}
    enabled: ${CERT_SERVICE_ENABLED:false}  # set true when cert service is running
```

### 6. Flyway Migration

`V20260901_2000__seed_certificate_permissions.sql`

Adds `CERTIFICATE_VIEW`, `CERTIFICATE_ISSUE`, `CERTIFICATE_REVOKE`,
`CERTIFICATE_TEMPLATE_MANAGE` permissions and grants them to `ADMIN`.

---

## Environment Variables Added

| Variable | Description | Required |
|---|---|---|
| `SERVICE_KEY_SECRET` | Shared secret for internal API (`X-Service-Key`) | Yes |
| `CERT_SERVICE_URL` | Base URL of the cert service | When cert service enabled |
| `CERT_SERVICE_ENABLED` | Enable/disable webhook calls to cert service | No (default: false) |

---

## Security Notes

- The `/api/v1/internal/**` routes are excluded from the standard JWT filter
  chain and are instead protected solely by the `ServiceKeyAuthFilter`.
- The `X-Service-Key` must be at least 32 characters and treated as a secret.
- The JWKS endpoint is public but returns no secret material.
- Webhook calls from LMS-BackEnd → cert service use the same `SERVICE_KEY_SECRET`
  in the `X-Service-Key` header.
