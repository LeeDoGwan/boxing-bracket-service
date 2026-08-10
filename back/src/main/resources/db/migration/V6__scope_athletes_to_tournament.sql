ALTER TABLE athletes
    ADD COLUMN tournament_id BIGINT NULL;

CREATE INDEX idx_athletes_tournament_name
    ON athletes(tournament_id, name);
