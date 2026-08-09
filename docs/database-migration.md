# Database Migration Policy

Last updated: 2026-08-08

## Scope

MariaDB schema changes are managed by Flyway. The execution source is
`back/src/main/resources/db/migration/`; Hibernate only validates the schema.
No application API or deployment automation is introduced by this policy.

The backend pins Flyway `9.22.3` and its `flyway-mysql` support module while
using Spring Boot 2.7.18. This supports the MariaDB 10.11 CI smoke test. The
current baseline is:

| Version | File | Contents |
| --- | --- | --- |
| `V1` | `V1__create_initial_schema.sql` | Initial JPA tables, tournament `judge_count` (`3` or `5`), MariaDB-compatible `LONGTEXT` audit payloads, optimistic-lock columns, workflow uniqueness constraints, schedule/staff indexes, and audit indexes |
| `V2` | `V2__add_penalty_round_reference.sql` | Adds nullable `penalties.round_no` for round-level penalty history while totals remain bout-level |
| `V3` | `V3__add_unique_tournament_bout_number.sql` | Enforces unique bout numbers within each tournament |
| `V4` | `V4__add_bout_import_idempotency.sql` | Adds persistent import batch/row keys and a unique retry constraint |
| `V5` | `V5__add_bout_schedule_indexes.sql` | Adds composite indexes for tournament and ring bout schedule queries |

The baseline covers `accounts`, `tournaments`, `athletes`, `rings`, `bouts`,
`round_scores`, `penalties`, `bout_results`, `notices`, `schedule_items`,
`staff_assignments`, and `audit_logs`.

The current model stores cross-aggregate references as scalar IDs and does not
declare JPA associations. V1 therefore does not add foreign keys that the
application does not currently own. Audit rows also remain independent so
history survives deletion of a referenced business record.

The application preserves scalar-reference integrity at service boundaries:
tournament, ring, athlete, and account deletes are rejected while owned records
remain; bout deletes reject scoring, active-ring, and schedule references; and a
ring cannot move to another tournament after creation. Audit rows are the
explicit exception because they intentionally preserve historical identifiers.
Bout CSV/Excel imports require an `Idempotency-Key`; imported rows persist that
key with their source row number, and a repeated key returns the existing rows.

## Runtime Rules

- Local MariaDB and test H2 profiles enable Flyway and validate on migrate.
- `ddl-auto: validate` is used in both profiles. Hibernate does not create,
  update, or drop tables.
- `baseline-on-migrate: false` and `out-of-order: false` prevent an unknown
  schema or skipped version from being accepted silently.
- `validate-on-migrate: true` rejects checksum changes and invalid migration
  state before the application context starts.
- Already applied migration files are immutable. Add a higher version for every
  schema change; never edit an applied file to repair production data.
- MariaDB is the operational database. H2 runs the same V1 through V5 SQL in
  MySQL compatibility mode for fast repository and context tests.

## New Installation

1. Create an empty `boxing_bracket` MariaDB database and an application account
   with only the privileges required by Flyway and the service.
2. Configure the local datasource without committing credentials.
3. Start the backend from `back/` with `mvn spring-boot:run`.
4. Confirm the Flyway log reports V5 as the current schema version and the
   health endpoint returns `UP`.
5. Record the deployed application and schema versions in the environment
   change record.

The repository does not contain real hostnames, passwords, or production
connection information.

## Existing Database

The repository has no deployed shared MariaDB database at this MVP stage. The
baseline changes in V1 are therefore safe before first deployment; after the
first deployment, V1 is immutable and all schema changes require a new version.
New installations apply V1 through V5 in order. Do not assume that an
existing database matches any version.

Before first startup against an existing database:

1. Take and verify a database backup.
2. Inspect table, column, index, unique-constraint, and data-type definitions.
3. Compare the result with V1 through V5. Before applying V3, find and resolve
   duplicate `(tournament_id, bout_number)` values.
4. If the schema is equivalent to V1, perform the approved baseline operation at
   V1 and let Flyway apply V2 through V5. If it is equivalent to V2, baseline at V2
   only through the approved procedure and let Flyway apply V3 through V5. If it is
   equivalent to V3, baseline at V3 and let Flyway apply V4 and V5. If it is
   equivalent to V4, baseline at V4 and let Flyway apply V5. Then start the
   application with normal validation settings.
5. If it is not equivalent, write a reviewed forward migration or a dedicated
   data conversion plan. Do not enable `baseline-on-migrate` to bypass the
   difference.

Flyway SQL migrations do not automatically roll back partially applied business
changes. Recovery uses the approved backup/restore or a reviewed forward-fix
migration, depending on the incident and database transaction behavior.

## Failure Handling

Application startup must stop for checksum mismatch, missing migration
privileges, a failed migration, an unexpected existing table/index, or an
application/schema version mismatch. Operators should inspect Flyway history,
database locks, and the failed SQL before retrying. Never delete the Flyway
history table as a repair step.

The old `docs/database-migration-*.sql` files are retained as historical
pointers only. They contain no executable DDL; the Flyway directory is the
single schema execution source.

## Change Checklist

- Update the relevant Entity and repository tests.
- Add one new versioned SQL file with the next version.
- Check table/column names against the JPA physical naming strategy.
- Verify MariaDB syntax and the H2 compatibility path.
- Run `cd back && mvn -q test` and `git diff --check`.
- Update this document, `docs/design.md`, `docs/testing.md`, and `README.md`.
- Review backup, approval, and rollback/forward-fix requirements before a
  production application.
