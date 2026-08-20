# Working with migrations on the shared database

One Supabase project is shared by the whole team, and Flyway owns every table.
Nobody creates or alters a table by hand — not in the Supabase table editor, not
in a SQL console. If it is not in `src/main/resources/db/migration`, it does not
exist.

That gives one reproducible schema for everyone. It also means a mistake is
everyone's mistake, so a few conventions matter more here than they would on a
private database.

## Naming: timestamps, not sequence numbers, from now on

The baseline `V1`–`V6` stays as it is — written together, so it cannot collide. **Every new migration uses a UTC timestamp version:**

```
V20260820_1430__add_enrollment_tables.sql
V20260821_0915__add_enrollment_status.sql
```

Sequential numbering breaks the moment two people work in parallel: both write
`V8__…`, both branches merge, and Flyway refuses to start for everyone with
`Found more than one migration with version 8`. A timestamp cannot collide, and
Flyway still orders it correctly after `V6` because versions compare
numerically, part by part.

Get the version with:

```bash
date -u +V%Y%m%d_%H%M
```

## The three rules

**1. Never edit a migration that has been applied.** Flyway stores a checksum.
Changing an applied file makes every other developer's app refuse to start with
a checksum mismatch. Fix forward with a new migration instead — always, even for
a typo in a comment.

**2. Every migration is additive and reversible by a follow-up.** Adding a
`NOT NULL` column to a populated table, renaming a column, or dropping one will
break whoever is still on the previous branch, because their code no longer
matches the schema. Split it: add nullable → backfill → enforce, across separate
migrations that ship with the code that needs them.

**3. Run the app after pulling.** Migrations apply at startup. If you pull a
branch with a new migration and someone else has not, they will hit
`Schema-validation: missing table` when they next start. That is
`ddl-auto: validate` doing its job, and the fix is always "pull and restart".

## Commands

The Flyway Maven plugin talks to the same database without booting the app. It
cannot read `.env`, so export the variables first:

```bash
export $(grep -v '^#' .env | grep -E '^DB_' | xargs)
```

| Command | Use |
| --- | --- |
| `mvn flyway:info` | What is applied, what is pending, in what order |
| `mvn flyway:validate` | Checksum and ordering check without applying anything |
| `mvn flyway:migrate` | Apply pending migrations without starting the app |
| `mvn flyway:repair` | Fix the history table after a failed or edited migration |

`flyway:clean` is disabled in the plugin and in every profile. On a shared
database it would drop everyone's schema.

## When it goes wrong

**Checksum mismatch.** Someone edited an applied migration. Ask them to revert
the file to what was applied and add a new migration instead. If the edit is
already merged and harmless, `mvn flyway:repair` realigns the stored checksums —
run it once, and tell the team, because everyone's history table needs it.

**"Found more than one migration with version N".** Two migrations share a
version, usually from parallel branches. Rename one to a timestamp version.
Check `target/classes/db/migration` too — a stale copy of a renamed file
produces the same error until `mvn clean`.

**A migration failed halfway.** PostgreSQL runs DDL transactionally, so Flyway
rolls the statement back, but the history table keeps a failed entry that blocks
further migrations. `mvn flyway:repair` removes it; then fix the SQL and
re-apply.

**Flyway reports a migration in the history that is not on disk.** Someone
deleted or renamed a migration that had already been applied. `mvn flyway:repair`
drops the orphaned history row; any table it created has to be dropped by hand.

**Someone needs a clean slate.** Do not clean the shared database. Create a
personal Supabase project, point `.env` at it, and let Flyway build the schema
from scratch.

## The trade-off you are accepting

A shared development database buys one consistent schema and no local Postgres
to install. In exchange:

- A broken migration blocks the whole team until it is fixed.
- Test data is common ground. Anyone can delete the user you were testing with,
  and there is no isolation between people working on different features.
- `BOOTSTRAP_ADMIN_PASSWORD` guards a real, internet-reachable database. Treat
  it like a production credential, not a dev placeholder.

The first two are the ones that will actually cost time. If they start to, the
answer is a Supabase project per developer with the same migrations — the setup
does not change, only `DB_URL`.
