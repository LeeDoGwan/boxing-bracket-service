-- Staff ring assignment migration for MariaDB.
-- Apply this migration before enabling staff assignment enforcement.

CREATE TABLE staff_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    account_id BIGINT NOT NULL,
    tournament_id BIGINT NOT NULL,
    ring_id BIGINT NOT NULL,
    role VARCHAR(255) NOT NULL,
    active BIT NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_staff_assignments_account_tournament_ring (account_id, tournament_id, ring_id),
    INDEX idx_staff_assignments_tournament_active (tournament_id, active),
    INDEX idx_staff_assignments_account_active (account_id, active),
    INDEX idx_staff_assignments_ring_active (ring_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
