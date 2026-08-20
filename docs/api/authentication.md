# Authentication & Authorization (Module 1)

Implements the Module 1 specification: administrator-issued temporary passwords,
JWT access tokens, rotating refresh sessions, RBAC, and security tracking.

## Token model

| | Access token | Refresh token |
| --- | --- | --- |
| Format | JWT (HS256) | Opaque random, 256 bits |
| Lifetime | 15 min (`JWT_ACCESS_TTL`) | 7 days (`JWT_REFRESH_TTL`) |
| Stored server-side | No | SHA-256 digest in `user_session` |
| Revocable | No — expires only | Yes, immediately |
| Sent on | Every protected request | Only `/auth/refresh` and `/auth/logout` |

The refresh token is deliberately **not** a JWT. The ERD stores
`refresh_token_hash`, and a digest is only meaningful for opaque material.
Making it opaque is also what makes server-side revocation possible.

**Access token claims**

```
sub          user email
iss          lms-backend
uid          user id
sid          session id (the token family)
roles        ["INSTRUCTOR"]
permissions  ["COURSE_VIEW", "COURSE_CREATE"]
typ          "access"
iat / exp / jti
```

Authorities are carried in the token, so a protected request costs no database
round trip. The trade-off: a role change takes effect for an already-issued
access token only when that token expires — bounded by the 15-minute TTL.

The token never carries the password hash. `LmsUserDetails.fromClaims(...)`
builds the request principal with a null credential.

## Endpoints

| Method | Path | Access | Purpose |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/login` | public | Email + password → token pair |
| POST | `/api/v1/auth/refresh` | public | Rotate refresh token → new pair |
| POST | `/api/v1/auth/logout` | authenticated | Revoke the current session |
| POST | `/api/v1/auth/logout-all` | authenticated | Revoke every session |
| GET | `/api/v1/auth/me` | authenticated | Principal + roles + permissions |
| GET | `/api/v1/auth/sessions` | authenticated | List own live sessions |
| DELETE | `/api/v1/auth/sessions/{id}` | authenticated | Revoke one own session |
| POST | `/api/v1/auth/forgot-password` | public | Request a reset link |
| POST | `/api/v1/auth/reset-password` | public | Redeem a reset token |
| POST | `/api/v1/invitations` | `INVITATION_CREATE` | Create account, email its temporary password |
| POST | `/api/v1/invitations/{id}/resend` | `INVITATION_MANAGE` | New temporary password, resend |
| DELETE | `/api/v1/invitations/{id}` | `INVITATION_MANAGE` | Withdraw invitation |

`/refresh`, `/forgot-password` and `/reset-password` are public because the
caller has no access token at that point — the refresh or reset token *is* the
credential. Onboarding needs no public endpoint: the invitee signs in with the
temporary password like any other user.

## Login flow

1. Normalise the email; read the account.
2. If the account is locked → `403 ACCOUNT_DISABLED`. Checked **before**
   credentials so a locked account cannot be probed by password guessing.
3. Verify the password through `AuthenticationManager`.
4. Record the attempt in `login_attempt` (own transaction, so a failure is
   still recorded when the login transaction rolls back).
5. On the Nth consecutive failure → set `is_locked`, write
   `account_status_history` + `audit_log`.
6. On success → open a `user_session`, mint the access token, return both.

An account still on its temporary password signs in normally but comes back
with `mustChangePassword: true` and a token that authorizes nothing but the
password change. If the invitation has expired, the temporary password is dead
and login returns `403 ACCOUNT_DISABLED`.

## Refresh rotation

```
login          → access A + refresh R1        (session S)
A expires, R1  → access B + refresh R2        (same session S)
R1 replayed    → 401 TOKEN_INVALID
```

Rotation replaces `refresh_token_hash` **in place** on the session row, so the
session id is the token family and exactly one refresh token is valid per
session at any moment.

**Known limit:** replaying a retired token is rejected, but it cannot be
distinguished from an ordinary invalid token, so it does not trigger
family-wide revocation. Detecting that requires either a token-history table or
a `family_id` column, neither of which is in the ERD. Left as a deliberate gap
rather than an undocumented deviation.

## Logout

Revokes the session row. The access token stays technically valid until it
expires (≤ 15 min), which is the documented trade-off of a stateless access
token. `sid` is present in the token if immediate revocation is added later.

A password change or reset revokes **every** session for that user.

## Lockout policy

`LOGIN_MAX_FAILED_ATTEMPTS` (5) consecutive failures within
`LOGIN_FAILED_WINDOW` (15 min) locks the account. "Consecutive" is measured
from the later of the window start and the last successful login. Unlocking is
an administrator action (`POST /users/{id}/unlock`, `USER_LOCK`) — there is no
automatic expiry of the lock.

## Onboarding: temporary password

An administrator creates the account; the invitee receives a temporary password
by email and is forced to replace it on first use. There is exactly one path in.

```
ADMIN → POST /invitations {name, email, role}
      → users row: bcrypt(temporary password), is_active TRUE, role assigned
      → user_invitation row: expires_at
      → email to the INVITEE ONLY, carrying the temporary password

USER  → POST /auth/login  →  200 { mustChangePassword: true, restricted token }
      → POST /users/me/password {currentPassword: temp, newPassword: …}
      → accepted_at stamped, onboarding complete
```

**A pending invitation is the "temporary password still in force" flag.** The
ERD has no `must_change_password` column and none was added: an invitation row
with `accepted_at IS NULL` already means exactly that, and its `expires_at`
gives the temporary password an expiry for free.

**The administrator never sees the temporary password.** It is generated
server-side, hashed immediately, and appears only in the email to the invitee —
so there is no window in which a second person holds a working credential. A
test asserts the create-invitation response does not contain it.

**While the temporary password is live, the token grants nothing.** Login
succeeds and returns `mustChangePassword: true`, but the access token carries no
roles and no permissions, so every `@PreAuthorize` endpoint denies it.
`PasswordChangeRequiredFilter` closes the remaining gap — the endpoints that
only require authentication — with an explicit allowlist:

```
POST /users/me/password    GET  /auth/me
POST /auth/logout          POST /auth/logout-all
```

Anything else returns `403 PASSWORD_CHANGE_REQUIRED`. Refresh re-derives the
restriction from the database, so it cannot be used to trade a restricted token
for a full one.

**An expired invitation kills the temporary password**, so the credential never
outlives its window: login returns `403 ACCOUNT_DISABLED` with a message to ask
for a resend. `POST /invitations/{id}/resend` issues a new temporary password
and a new expiry, retiring the old one.

Revoking an unaccepted invitation deletes both the invitation and the account,
because the ERD has no revoked flag and the account exists solely because of
that invitation.

### Re-introducing a set-password link later

A link route existed and was removed deliberately to keep one path in. The
groundwork is still there, so bringing it back is additive:

- `user_invitation.token_hash` is still minted and stored on every invite. The
  ERD requires the column, and the value is inert while nothing sends it.
- What was removed: `POST /invitations/accept`, `GET /invitations/validate`,
  their DTOs, the two entries in `SecurityConfig.PUBLIC_ENDPOINTS`, and
  `AppProperties.invitationLink(...)`.
- Both routes converged on the same completion step — stamping `accepted_at` —
  which is now driven by `PasswordChangedEvent`, so a new route only has to
  reach that same point.

## Authorization

```
users → user_role → roles → role_permission → permissions
```

Both roles (as `ROLE_<NAME>`) and permission names are granted as authorities,
so endpoints use `@PreAuthorize("hasAuthority('COURSE_CREATE')")` directly.
`PermissionEvaluator` additionally backs the object-level
`hasPermission(target, 'ACTION')` form.

`user_role` is mapped as an entity rather than a plain `@ManyToMany` because the
join table carries `assigned_at` and `assigned_by`.

## Audit

`audit_log` records: `LOGIN_SUCCESS`, `LOGIN_FAILED`, `LOGOUT`,
`TOKEN_REFRESHED`, `PASSWORD_CHANGED`, `PASSWORD_RESET_REQUESTED`,
`PASSWORD_RESET`, `ROLE_ASSIGNED`, `ROLE_REMOVED`, `ACCOUNT_LOCKED`,
`ACCOUNT_UNLOCKED`, `ACCOUNT_ACTIVATED`, `ACCOUNT_DEACTIVATED`, `USER_INVITED`,
`INVITATION_ACCEPTED`, `INVITATION_REVOKED`, `INVITATION_RESENT`,
`SESSION_REVOKED`.

## Bootstrapping

Inviting a user requires an authenticated administrator, so the first one is
created at startup from `BOOTSTRAP_ADMIN_*` when
`BOOTSTRAP_ADMIN_ENABLED=true`. Enable once on a fresh environment, sign in,
then set it back to false.

## Operational notes

- `MAIL_ENABLED=false` writes messages to the log instead of sending them.
  Leaving it false in an environment with real users means invitations are
  never delivered.
- Expired sessions accumulate in `user_session`; `SessionService.purgeExpired()`
  exists but is **not** scheduled. Wire it to a job or a nightly task.
- There is no rate limit on `/auth/login` beyond the per-account lockout. A
  distributed attack across many addresses is not throttled.
