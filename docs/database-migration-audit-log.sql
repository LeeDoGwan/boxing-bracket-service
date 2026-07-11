-- Administrator audit log migration for MariaDB.
-- Apply this migration before deploying the audit logging feature.

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tournament_id BIGINT NULL,
    actor_account_id BIGINT NULL,
    actor_username VARCHAR(255) NULL,
    actor_role VARCHAR(255) NULL,
    action_type VARCHAR(255) NOT NULL,
    target_type VARCHAR(255) NOT NULL,
    target_id BIGINT NULL,
    ring_id BIGINT NULL,
    bout_id BIGINT NULL,
    deduplication_key VARCHAR(64) NULL,
    before_data LONGTEXT NULL,
    after_data LONGTEXT NULL,
    ip_address VARCHAR(255) NULL,
    user_agent VARCHAR(512) NULL,
    success BIT NOT NULL,
    failure_reason VARCHAR(512) NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_audit_logs_deduplication_key (deduplication_key),
    INDEX idx_audit_logs_tournament_created (tournament_id, created_at),
    INDEX idx_audit_logs_actor_created (actor_account_id, created_at),
    INDEX idx_audit_logs_bout_created (bout_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
