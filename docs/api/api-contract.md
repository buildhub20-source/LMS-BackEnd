# API Contract

Base path: `/api/v1`. Interactive documentation: `/swagger-ui.html`.

## Response envelope

Every successful response is wrapped:

```json
{
  "success": true,
  "message": "Invitation sent",
  "data": { },
  "timestamp": "2026-08-20T06:00:00Z"
}
```

Paged endpoints put a `PageResponse` in `data`:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "last": true
}
```

## Error contract

```json
{
  "success": false,
  "code": "VALIDATION_FAILED",
  "message": "Request validation failed",
  "path": "/api/v1/users",
  "errors": [{ "field": "email", "message": "must be a well-formed email address" }],
  "timestamp": "2026-08-20T06:00:00Z"
}
```

| `code` | HTTP | Meaning |
| --- | --- | --- |
| `VALIDATION_FAILED` | 400 | A request field failed validation; see `errors`. |
| `BAD_REQUEST` | 400 | The body or a parameter could not be read. |
| `UNAUTHENTICATED` | 401 | No usable bearer token. |
| `INVALID_CREDENTIALS` | 401 | Wrong email or password. |
| `TOKEN_INVALID` / `TOKEN_EXPIRED` | 401 | The token failed verification. |
| `ACCESS_DENIED` | 403 | Authenticated but missing the required permission. |
| `ACCOUNT_DISABLED` | 403 | The account is suspended or deactivated. |
| `RESOURCE_NOT_FOUND` | 404 | No such resource. |
| `RESOURCE_ALREADY_EXISTS` | 409 | A uniqueness rule would be broken. |
| `BUSINESS_RULE_VIOLATION` | 422 | Well-formed, but not allowed by a business rule. |
| `INTERNAL_ERROR` | 500 | Unexpected failure; details are logged, not returned. |

## Authentication

```
POST   /api/v1/auth/login            { email, password }   -> tokens + user
POST   /api/v1/auth/refresh          { refreshToken }      -> rotated tokens
POST   /api/v1/auth/logout           { refreshToken? }     -> revokes the session
POST   /api/v1/auth/logout-all                             -> revokes every session
GET    /api/v1/auth/me                                     -> user, roles, permissions
GET    /api/v1/auth/sessions                               -> live sessions
DELETE /api/v1/auth/sessions/{id}                          -> revoke one session
POST   /api/v1/auth/forgot-password  { email }             -> always 200
POST   /api/v1/auth/reset-password   { token, newPassword }
```

Send the access token as `Authorization: Bearer <token>` on every other call.
`GET /auth/me` returns the permission list the frontend uses to decide what to
render; it is a convenience, not a security control.

## Endpoints and required permissions

| Method and path | Permission |
| --- | --- |
| `GET /users`, `GET /users/{id}` | `USER_VIEW` (or self) |
| `PATCH /users/{id}` | `USER_UPDATE` (or self) |
| `PUT /users/{id}/roles` | `USER_MANAGE_ROLES` |
| `POST /users/me/password` | authenticated |
| `POST /users/{id}/deactivate` | `USER_DELETE` |
| `POST /users/{id}/activate` | `USER_UPDATE` |
| `POST /users/{id}/lock`, `/unlock` | `USER_LOCK` |
| `GET /users/{id}/status-history` | `USER_VIEW` |

There is no `POST /users`: accounts are created by `POST /invitations`, which is
the only path that produces an activated account.
| `GET /roles`, `GET /roles/{id}` | `ROLES_VIEW` |
| `POST /roles`, `PATCH /roles/{id}`, `DELETE /roles/{id}` | `ROLES_MANAGE` |
| `GET /permissions`, `GET /permissions/{id}` | `PERMISSIONS_VIEW` |
| `POST /permissions`, `PATCH /permissions/{id}`, `DELETE /permissions/{id}` | `PERMISSIONS_MANAGE` |
| `POST /invitations` | `INVITATION_CREATE` |
| `GET /invitations`, `GET /invitations/{id}` | `INVITATION_VIEW` |
| `POST /invitations/{id}/resend`, `DELETE /invitations/{id}` | `INVITATION_MANAGE` |

Onboarding has no public endpoint: the invitee signs in with the temporary
password from their invitation email and is forced to replace it via
`POST /users/me/password`.
| `POST /courses` | `COURSE_CREATE` |
| `GET /courses`, `GET /courses/{id}`, `GET /courses/instructor/{id}` | `COURSE_VIEW` |
| `PATCH /courses/{id}`, `POST /courses/{id}/archive` | `COURSE_UPDATE` |
| `POST /courses/{id}/publish` | `COURSE_PUBLISH` |
| `DELETE /courses/{id}` | `COURSE_DELETE` |

## Paging and sorting

List endpoints accept `page`, `size` and `sort` (for example
`?page=0&size=20&sort=createdAt,desc`). `size` is capped at 100 and the default
sort is `createdAt` descending.

## Conventions

- Timestamps are UTC ISO-8601.
- Identifiers are UUIDs.
- `POST` that creates a resource returns `201` with a `Location` header.
- `PATCH` applies partial updates; `null` fields are left unchanged.
