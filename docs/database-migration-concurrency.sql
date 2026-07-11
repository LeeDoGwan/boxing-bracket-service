-- Run once against the MariaDB database before deploying the concurrency update.
-- Resolve duplicate legacy rows before applying the two unique constraints.

ALTER TABLE bouts
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE rings
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE round_scores
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE bout_results
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE round_scores
    ADD CONSTRAINT uk_round_scores_bout_round_judge
    UNIQUE (bout_id, round_no, judge_id);

ALTER TABLE bout_results
    ADD CONSTRAINT uk_bout_results_bout
    UNIQUE (bout_id);
