-- Tournament schedule migration for MariaDB.
-- Apply this migration before deploying the schedule management feature.

CREATE TABLE schedule_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tournament_id BIGINT NOT NULL,
    ring_id BIGINT NULL,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    start_time DATETIME(6) NOT NULL,
    end_time DATETIME(6) NULL,
    related_bout_id BIGINT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NULL,
    updated_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    INDEX idx_schedule_items_tournament_start (tournament_id, start_time, id),
    INDEX idx_schedule_items_ring_start (ring_id, start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
