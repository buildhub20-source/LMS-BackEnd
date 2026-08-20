# Database Design

PostgreSQL 16, locally or on Supabase — see
[supabase.md](supabase.md) for the hosted setup. The schema Flyway owns is
whichever `DB_SCHEMA` names (`public` locally, a dedicated `lms` schema on
Supabase so the tables are not published through its REST API).

The schema is owned by Flyway migrations in
`src/main/resources/db/migration`; Hibernate runs with `ddl-auto: validate`
outside tests, so the application refuses to start if the code and the schema
disagree. `MigrationSchemaCheckTest` runs the real migrations and the same
validation in CI, so a column renamed in an entity but not in a migration fails
the build rather than a deployment.

**The Module 1 ERD is the source of truth.** Where the implementation needed
something the ERD does not model, the workaround is documented below rather
than solved by adding a column.

## Entity relationships

```
users ──< user_role >── roles ──< role_permission >── permissions
  │
  ├──< user_session            (refresh sessions)
  ├──< user_invitation         (also FK invited_by → users)
  ├──< password_reset_token
  ├──< login_attempt           (nullable: unknown addresses are recorded too)
  ├──< account_status_history  (also FK changed_by → users)
  └──< audit_log               (nullable actor)
```

## Tables

### Core

| Table | Purpose | Notable columns |
| --- | --- | --- |
| `users` | Accounts | `email` unique; `password` **nullable** until the invitation is accepted; `is_active`, `is_locked` |
| `roles` | Named permission bundles | `name` unique |
| `permissions` | Fine-grained capabilities | `name` unique; `(resource, action)` unique |
| `user_role` | Role assignment | Composite PK; carries `assigned_at`, `assigned_by` |
| `role_permission` | Permission assignment | Composite PK; carries `created_at` |

### Authentication

| Table | Purpose | Notable columns |
| --- | --- | --- |
| `user_session` | Refresh-token sessions | `refresh_token_hash` unique (SHA-256); `is_revoked`; `expires_at`; `last_used_at` |
| `user_invitation` | Onboarding tokens | `token_hash` unique; `expires_at`; `accepted_at` |
| `password_reset_token` | Reset tokens | `token_hash` unique; `is_used`; short `expires_at` |

### Security tracking

| Table | Purpose | Notable columns |
| --- | --- | --- |
| `login_attempt` | Every attempt, success or not | `email` recorded even with no matching account; drives lockout |
| `account_status_history` | Status transitions | `status` ∈ ACTIVE/INACTIVE/LOCKED/DELETED; `changed_by`, `reason` |
| `audit_log` | Security-relevant actions | `action`, `resource`, `resource_id`, `details`, `ip_address` |

## Design decisions

**Email is stored lowercase, and the database enforces it.** Services lowercase
every address before writing, but `CHECK (email = lower(email))` is what makes a
direct insert unable to create a second account differing only by case — which
`UNIQUE (email)` alone would allow and `lower(email)` lookups would then miss.
A functional unique index would express this more directly, but H2 does not
support one, and keeping the migration test runnable is worth more here.

**Tokens are stored as digests, never in plaintext.** `user_session`,
`user_invitation` and `password_reset_token` all hold SHA-256 of the raw token.
SHA-256 rather than BCrypt is deliberate: these are 256 bits of `SecureRandom`
output, so they are not dictionary-attackable, and lookup is by digest on every
refresh — a slow hash would have to be recomputed per candidate row.

**`users.password` is nullable by design.** An administrator creates the account
and the invitation together; the password arrives only at acceptance. A null
password can never authenticate, because `is_active` is false until then and
`CustomUserDetailsService` rejects it independently.

**No `created_by` / `updated_by` columns.** The ERD records *who did what* in
`audit_log`, not in per-row columns, so `Timestamped` provides only
`created_at` / `updated_at`.

**No `@Version` column.** The ERD does not model optimistic locking. Concurrent
edits to the same user row are last-write-wins.

**`user_role` is a JPA entity, not a plain `@ManyToMany`.** The join table
carries `assigned_at` and `assigned_by`; a plain many-to-many would leave
`assigned_by` permanently null. `role_permission` has no actor column, so a
plain many-to-many is used there and `created_at` comes from its default.

**Invitation status is derived, not stored.** The ERD has `accepted_at` and
`expires_at` but no status column, so `InvitationStatus` is computed from them
and the two can never disagree.

**Role protection is by name, not by flag.** There is no `system_role` column,
so `SystemRoles.PROTECTED` guards ADMIN / INSTRUCTOR / STUDENT from deletion.

**Revoking an invitation deletes rows.** With no revoked flag available,
withdrawing an unaccepted invitation deletes the invitation and the pending
account it created. Accepted invitations cannot be revoked — deactivate the
user instead.

## Indexes

Matching the ERD's indexing recommendations:

```
users.email (unique)                    users.is_active
user_role.user_id                       user_role.role_id
role_permission.role_id                 role_permission.permission_id
user_session.refresh_token_hash (uniq)  user_session.user_id, .expires_at
user_invitation.token_hash (unique)     user_invitation.user_id, .expires_at
password_reset_token.token_hash (uniq)  password_reset_token.user_id
login_attempt (email, attempted_at)
audit_log (resource, created_at)        audit_log.user_id
account_status_history.user_id, .changed_at
```

## Migrations

| Version | Contents |
| --- | --- |
| `V1` | `users`, `account_status_history` |
| `V2` | `roles` |
| `V3` | `permissions`, `user_role`, `role_permission` |
| `V4` | `user_session`, `user_invitation`, `password_reset_token` |
| `V5` | `login_attempt`, `audit_log` |
| `V6` | Seed roles, permissions and role-permission grants |

New migrations use timestamp versions (`V20260820_1430__…`), not sequence
numbers, because the database is shared and parallel branches would otherwise
collide on `V8`. See [migrations.md](migrations.md) for the full workflow.

Migrations use standard SQL so they run on both PostgreSQL and the H2
PostgreSQL-compatibility mode used by the schema check. Anything genuinely
PostgreSQL-specific still needs verification against a real database —
Testcontainers is not wired up yet.

## Retention

`login_attempt`, `audit_log` and expired `user_session` rows grow without
bound. `SessionService.purgeExpired()` exists but is not scheduled, and there
is no retention job for the two append-only tables yet.
