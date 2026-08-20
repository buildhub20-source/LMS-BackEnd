# Backend Architecture Overview

## Shape

Feature-oriented packages with layers inside each feature:

```
com.lms
├── config/      application-wide Spring and infrastructure configuration
├── security/    authentication and authorization infrastructure
├── common/      genuinely cross-feature building blocks
└── <feature>/   controller · service · repository · entity · dto · mapper
```

Module 1 features: `user`, `role`, `permission`, `invitation`, `auth`.

## Request flow

```
HTTP request
  └─ JwtAuthenticationFilter        establishes the security context from the bearer token
      └─ SecurityFilterChain        path-level authorization
          └─ @PreAuthorize          permission-level authorization
              └─ Controller         HTTP concerns only, validates the request DTO
                  └─ Service        business rules, transactions, orchestration
                      └─ Repository persistence
                          └─ Mapper entity → response DTO
```

Errors from any layer are translated by `common/exception/GlobalExceptionHandler`
into the `ApiError` contract, so no layer builds error responses by hand.

## Rules that keep the shape

| Rule | Where it is enforced |
| --- | --- |
| Controllers stay thin | Controllers only call one service method and wrap the result. |
| Entities never leave the service layer | Every controller returns a DTO or `PageResponse`. |
| Persistence stays in repositories | Services never build JPQL or use `EntityManager`. |
| `common/` stays small | Feature-owned validation and constants live in the feature (see `role/constants`, `user/event`). |
| Backend is the security boundary | Every non-public endpoint carries a `@PreAuthorize` permission check. |

## Authentication and authorization

- `auth/` is the API module: login, refresh, current user, logout.
- `security/` is the infrastructure: token issuing/parsing, principal loading,
  permission evaluation.
- A JWT carries the user id, roles and permissions. `LmsUserDetails` converts
  those into Spring authorities: roles become `ROLE_<NAME>`, permissions are
  used verbatim. Endpoints therefore authorize with `hasAuthority('COURSE_CREATE')`.
- Role-management permissions are named `ROLES_*` and `PERMISSIONS_*` so that a
  permission can never collide with the `ROLE_` authority prefix.

## Adding a feature

1. Create `com.lms.<feature>` with `controller`, `service`, `repository`,
   `entity`, `dto`, `mapper`. Add `validation/`, `constants/`, `event/` only
   when the module actually needs them.
2. Add a Flyway migration for the new tables.
3. Add the permissions the endpoints require to the seed migration.
4. Mirror the package under `src/test/java`.

## Decisions worth knowing

| Decision | Reason |
| --- | --- |
| Stateless JWT rather than sessions | The frontend is a separate SPA deployment. |
| Authorities embedded in the access token | Avoids a database round trip on every request; the short access TTL bounds staleness. |
| Refresh tokens are self-contained JWTs | Keeps the first release simple. Introduce a stored, revocable refresh token when logout-everywhere is required. |
| Flyway over `ddl-auto` | Schema changes are reviewable, ordered and repeatable. `ddl-auto` is `validate` outside tests. |
| MapStruct for mapping | Compile-time, no reflection, keeps mapping out of services. |
| Optimistic locking on every aggregate | `Auditable.version` prevents lost updates under concurrent edits. |
