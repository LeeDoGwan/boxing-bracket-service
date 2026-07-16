# Database Migration Policy

Last updated: 2026-07-16

## Scope

MariaDB schema changes are managed by Flyway. The execution source is
`back/src/main/resources/db/migration/`; Hibernate only validates the schema.
No application API or deployment automation is introduced by this policy.

The backend uses the Flyway version supplied by Spring Boot 2.7.18 dependency
management. The current baseline is:

| Version | File | Contents |
| --- | --- | --- |
| `V1` | `V1__create_initial_schema.sql` | All current JPA tables, optimistic-lock columns, workflow uniqueness constraints, schedule/staff indexes, and audit indexes |

The baseline covers `accounts`, `tournaments`, `athletes`, `rings`, `bouts`,
`round_scores`, `penalties`, `bout_results`, `notices`, `schedule_items`,
`staff_assignments`, and `audit_logs`.

The current model stores cross-aggregate references as scalar IDs and does not
declare JPA associations. V1 therefore does not add foreign keys that the
application does not currently own. Audit rows also remain independent so
history survives deletion of a referenced business record.

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
- MariaDB is the operational database. H2 runs the same V1 SQL in MySQL
  compatibility mode for fast repository and context tests.

## New Installation

1. Create an empty `boxing_bracket` MariaDB database and an application account
   with only the privileges required by Flyway and the service.
2. Configure the local datasource without committing credentials.
3. Start the backend from `back/` with `mvn spring-boot:run`.
4. Confirm the Flyway log reports V1 applied and the health endpoint returns
   `UP`.
5. Record the deployed application and schema versions in the environment
   change record.

The repository does not contain real hostnames, passwords, or production
connection information.

## Existing Database

The repository has no evidence of a deployed shared database, so V1 is the
new-installation baseline. Do not assume that an existing database matches it.

Before first startup against an existing database:

1. Take and verify a database backup.
2. Inspect table, column, index, unique-constraint, and data-type definitions.
3. Compare the result with V1 and resolve duplicate or incompatible legacy
   data before applying constraints.
4. If the schema is equivalent, perform an approved Flyway baseline operation
   at V1, then start the application with the normal validation settings.
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
