ALTER TABLE bouts
    ADD import_batch_key VARCHAR(100);

ALTER TABLE bouts
    ADD import_row_number INTEGER;

CREATE INDEX idx_bouts_import_batch_key ON bouts(import_batch_key);

ALTER TABLE bouts
    ADD CONSTRAINT uk_bouts_import_batch_row UNIQUE (import_batch_key, import_row_number);
