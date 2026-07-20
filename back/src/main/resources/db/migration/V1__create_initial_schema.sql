-- Initial schema baseline for MariaDB and the H2 test profile.
-- IDs remain application-managed references because the current JPA model does not
-- declare entity relationships or foreign-key constraints.

CREATE TABLE accounts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    login_id VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_accounts_login_id UNIQUE (login_id)
);

CREATE TABLE tournaments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    location VARCHAR(255),
    start_date DATE,
    end_date DATE,
    status VARCHAR(255) NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE athletes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    affiliation VARCHAR(255),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE rings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    tournament_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    current_bout_id BIGINT,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE bouts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    tournament_id BIGINT NOT NULL,
    ring_id BIGINT NOT NULL,
    bout_number INTEGER NOT NULL,
    match_type VARCHAR(255),
    red_athlete_id BIGINT NOT NULL,
    blue_athlete_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL,
    current_round INTEGER NOT NULL,
    total_rounds INTEGER,
    scheduled_order INTEGER,
    winner_side VARCHAR(255),
    result_confirmed BOOLEAN NOT NULL,
    event_bout BOOLEAN NOT NULL,
    started_at DATETIME(6),
    ended_at DATETIME(6),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE round_scores (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    bout_id BIGINT NOT NULL,
    round_no INTEGER NOT NULL,
    judge_id BIGINT NOT NULL,
    red_score INTEGER,
    blue_score INTEGER,
    status VARCHAR(255) NOT NULL,
    submitted_at DATETIME(6),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_round_scores_bout_round_judge UNIQUE (bout_id, round_no, judge_id)
);

CREATE TABLE penalties (
    id BIGINT NOT NULL AUTO_INCREMENT,
    bout_id BIGINT NOT NULL,
    target_side VARCHAR(255) NOT NULL,
    penalty_point INTEGER NOT NULL,
    reason VARCHAR(255),
    created_by BIGINT NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE bout_results (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    bout_id BIGINT NOT NULL,
    red_total_score INTEGER,
    blue_total_score INTEGER,
    red_penalty_total INTEGER,
    blue_penalty_total INTEGER,
    winner_side VARCHAR(255),
    decision_type VARCHAR(255),
    confirmed_by BIGINT NOT NULL,
    confirmed_at DATETIME(6),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_bout_results_bout UNIQUE (bout_id)
);

CREATE TABLE notices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tournament_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    active BOOLEAN NOT NULL,
    display_order INTEGER NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE TABLE schedule_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tournament_id BIGINT NOT NULL,
    ring_id BIGINT,
    type VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    start_time DATETIME(6) NOT NULL,
    end_time DATETIME(6),
    related_bout_id BIGINT,
    status VARCHAR(255) NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id)
);

CREATE INDEX idx_schedule_items_tournament_start
    ON schedule_items (tournament_id, start_time, id);
CREATE INDEX idx_schedule_items_ring_start
    ON schedule_items (ring_id, start_time);

CREATE TABLE staff_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL DEFAULT 0,
    account_id BIGINT NOT NULL,
    tournament_id BIGINT NOT NULL,
    ring_id BIGINT NOT NULL,
    role VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_staff_assignments_account_tournament_ring UNIQUE (account_id, tournament_id, ring_id)
);

CREATE INDEX idx_staff_assignments_tournament_active
    ON staff_assignments (tournament_id, active);
CREATE INDEX idx_staff_assignments_account_active
    ON staff_assignments (account_id, active);
CREATE INDEX idx_staff_assignments_ring_active
    ON staff_assignments (ring_id, active);

CREATE TABLE audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tournament_id BIGINT,
    actor_account_id BIGINT,
    actor_username VARCHAR(255),
    actor_role VARCHAR(255),
    action_type VARCHAR(255) NOT NULL,
    target_type VARCHAR(255) NOT NULL,
    target_id BIGINT,
    ring_id BIGINT,
    bout_id BIGINT,
    deduplication_key VARCHAR(64),
    before_data CLOB,
    after_data CLOB,
    ip_address VARCHAR(255),
    user_agent VARCHAR(512),
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(512),
    created_at DATETIME(6),
    updated_at DATETIME(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_audit_logs_deduplication_key UNIQUE (deduplication_key)
);

CREATE INDEX idx_audit_logs_tournament_created
    ON audit_logs (tournament_id, created_at);
CREATE INDEX idx_audit_logs_actor_created
    ON audit_logs (actor_account_id, created_at);
CREATE INDEX idx_audit_logs_bout_created
    ON audit_logs (bout_id, created_at);
