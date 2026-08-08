ALTER TABLE bouts
    ADD CONSTRAINT uk_bouts_tournament_bout_number UNIQUE (tournament_id, bout_number);
