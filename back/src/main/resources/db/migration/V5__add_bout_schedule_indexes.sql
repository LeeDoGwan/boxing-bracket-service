CREATE INDEX idx_bouts_tournament_scheduled
    ON bouts(tournament_id, scheduled_order);

CREATE INDEX idx_bouts_ring_scheduled
    ON bouts(ring_id, scheduled_order);
