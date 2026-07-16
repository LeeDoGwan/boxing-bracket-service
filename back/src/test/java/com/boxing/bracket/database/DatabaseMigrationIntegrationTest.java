package com.boxing.bracket.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseMigrationIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void appliesInitialMigrationAndLeavesNoPendingChanges() {
        assertThat(flyway.info().applied())
                .extracting(migration -> migration.getVersion().getVersion())
                .containsExactly("1");
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '1' AND success = TRUE",
                Integer.class
        )).isEqualTo(1);
    }

    @Test
    void createsEntityTablesAndOperationalConstraints() {
        assertThat(tableExists("accounts")).isTrue();
        assertThat(tableExists("tournaments")).isTrue();
        assertThat(tableExists("rings")).isTrue();
        assertThat(tableExists("bouts")).isTrue();
        assertThat(tableExists("round_scores")).isTrue();
        assertThat(tableExists("penalties")).isTrue();
        assertThat(tableExists("bout_results")).isTrue();
        assertThat(tableExists("notices")).isTrue();
        assertThat(tableExists("schedule_items")).isTrue();
        assertThat(tableExists("staff_assignments")).isTrue();
        assertThat(tableExists("audit_logs")).isTrue();

        assertThat(uniqueConstraintExists("round_scores", "uk_round_scores_bout_round_judge")).isTrue();
        assertThat(uniqueConstraintExists("bout_results", "uk_bout_results_bout")).isTrue();
        assertThat(uniqueConstraintExists("staff_assignments", "uk_staff_assignments_account_tournament_ring")).isTrue();
        assertThat(uniqueConstraintExists("audit_logs", "uk_audit_logs_deduplication_key")).isTrue();
        assertThat(columnExists("bouts", "version")).isTrue();
        assertThat(columnExists("rings", "version")).isTrue();
        assertThat(columnExists("round_scores", "version")).isTrue();
        assertThat(columnExists("bout_results", "version")).isTrue();
        assertThat(columnExists("staff_assignments", "version")).isTrue();
    }

    private boolean tableExists(String tableName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE LOWER(TABLE_NAME) = ?",
                Integer.class,
                tableName
        ) > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS "
                        + "WHERE LOWER(TABLE_NAME) = ? AND LOWER(COLUMN_NAME) = ?",
                Integer.class,
                tableName,
                columnName
        ) > 0;
    }

    private boolean uniqueConstraintExists(String tableName, String constraintName) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                        + "WHERE LOWER(TABLE_NAME) = ? AND LOWER(CONSTRAINT_NAME) = ? "
                        + "AND CONSTRAINT_TYPE = 'UNIQUE'",
                Integer.class,
                tableName,
                constraintName
        ) > 0;
    }
}
