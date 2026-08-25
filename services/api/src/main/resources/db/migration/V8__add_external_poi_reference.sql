ALTER TABLE poi
    ADD COLUMN external_provider VARCHAR(40),
    ADD COLUMN external_id VARCHAR(160);

CREATE UNIQUE INDEX idx_poi_external_reference
    ON poi(external_provider, external_id)
    WHERE external_provider IS NOT NULL AND external_id IS NOT NULL;
