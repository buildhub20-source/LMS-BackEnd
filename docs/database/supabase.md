# Supabase: the database in every environment

Supabase is hosted PostgreSQL, so the application talks to it as an ordinary
Postgres instance. It is the default and only configured target — `dev` and
`prod` differ in log verbosity, not in where the data lives.

There are **no localhost fallbacks**. `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
and `JWT_SECRET` have no defaults: a missing value fails at startup rather than
silently connecting to whatever Postgres happens to be running locally. A local
Postgres is still available through `docker compose --profile local-db up` for
offline work, but nothing should be verified only against it.

Four things differ from a local database and each needs a deliberate setting.

## Setup

1. In the Supabase dashboard: **Project Settings → Database → Connection string
   → JDBC**.
2. Copy `.env.example` to `.env` and fill in the Supabase block.
3. Run the app. Flyway creates the `lms` schema and applies `V1`–`V6` on first
   start.

```
DB_URL=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres?sslmode=require
DB_USERNAME=postgres.<project-ref>
DB_PASSWORD=<database password>
DB_SCHEMA=lms
DB_POOL_SIZE=5
```

`.env` is git-ignored and loaded through `spring.config.import` in
`application.yml` — no dependency and no IDE configuration needed.

## The four Supabase-specific decisions

### 1. Do not put our tables in `public`

This is the one that matters most. Supabase exposes the `public` schema through
its auto-generated PostgREST API. Any table there is reachable with the
project's `anon` key unless row-level security is enabled and policies deny
access.

Our schema holds bcrypt password hashes, refresh-token digests, invitation
tokens and the audit log. Leaving it in `public` publishes all of it to anyone
who has the anon key — which is a public value, shipped in the frontend bundle.

Setting `DB_SCHEMA=lms` puts every table in a schema PostgREST does not expose.
Flyway creates it (`create-schemas: true`), and both Flyway and Hibernate are
pointed at it from the same variable so they cannot drift apart.

If you ever move tables back to `public`, enable RLS on every one of them
first.

### 2. Session pooler, not transaction pooler

Supabase offers three endpoints:

| Endpoint | Port | Use it? |
| --- | --- | --- |
| Direct `db.<ref>.supabase.co` | 5432 | Only on an IPv6-capable network |
| Session pooler `...pooler.supabase.com` | 5432 | **Yes** |
| Transaction pooler `...pooler.supabase.com` | 6543 | No |

Flyway acquires a **session-scoped** `pg_advisory_lock` to serialise
migrations. Transaction pooling hands a different backend to each transaction,
so the lock is taken and lost immediately — migrations either fail or, worse,
two instances migrate concurrently. Transaction mode also disallows server-side
prepared statements, which PgJDBC uses by default.

The direct connection is IPv6-only on current Supabase projects; on an
IPv4-only network it fails to connect at all. The session pooler avoids both
problems.

### 3. TLS is required and enforced

`?sslmode=require` in the JDBC URL. PgJDBC defaults to `prefer`, which silently
downgrades to plaintext if negotiation fails — credentials and row data would
then cross the public internet in the clear.

`DataSourceSecurityCheck` refuses to start against a non-local Postgres URL
without `sslmode=require`, `verify-ca` or `verify-full`. Localhost and the
in-memory test database are exempt. `verify-full` is stronger than `require`
(it validates the server certificate against the hostname) and is worth moving
to once the CA bundle is pinned.

### 4. Keep the connection pool small

Supabase caps connections per project, and the cap is shared by every developer
pointing at the same instance. `DB_POOL_SIZE=5` leaves room; the default of 10
per developer exhausts a free-tier project quickly.

## What this changes about the workflow

**The database is shared.** With everyone on one Supabase project, one
developer running the app applies migrations for everybody, and any test data
is visible to all. Two consequences:

- A new migration lands the moment the first person starts the app. If someone
  else is on an older branch, Hibernate's `ddl-auto: validate` will refuse to
  start against the newer schema. That is the check working, but it will happen.
- Don't point the app at Supabase with `BOOTSTRAP_ADMIN_ENABLED=true` and a
  weak password. It is a real, internet-reachable database.

Giving each developer their own Supabase project, or a local Postgres for
day-to-day work with Supabase only for integration checks, avoids both. Worth
deciding before more people join the repo.

**Tests are unaffected.** The suite runs on in-memory H2 and never touches
Supabase, so the feedback loop stays fast and nobody's test run can damage
shared data. `MigrationSchemaCheckTest` still runs the real migrations against
H2 in PostgreSQL-compatibility mode.

That is also the remaining gap: nothing in CI exercises the migrations against
real PostgreSQL, and H2's compatibility mode is an approximation. Testcontainers
would close it.

## Troubleshooting

| Symptom | Cause |
| --- | --- |
| `Connection refused` / `Network is unreachable` | Using the direct IPv6 endpoint on an IPv4 network — switch to the session pooler |
| Flyway hangs or reports a lock error | Using the transaction pooler on 6543 — switch to 5432 |
| `password authentication failed` | Username must be `postgres.<project-ref>` for pooler endpoints, not plain `postgres` |
| `Schema-validation: missing table` at startup | Someone applied a newer migration; pull and rebuild |
| A backslash in the password is swallowed | `.env` is properties format — escape it as `\\` or change the password |
