ALTER TABLE poi ALTER COLUMN price_level TYPE INTEGER;

CREATE TABLE poi_search_document (
    poi_id UUID PRIMARY KEY REFERENCES poi(id) ON DELETE CASCADE,
    normalized_text TEXT NOT NULL,
    document_length INTEGER NOT NULL CHECK (document_length >= 0),
    indexed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE search_log (
    id UUID PRIMARY KEY,
    query_text VARCHAR(200) NOT NULL,
    normalized_query VARCHAR(200) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    radius_km NUMERIC(5,2) NOT NULL,
    result_count INTEGER NOT NULL CHECK (result_count >= 0),
    searched_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_search_log_searched_at ON search_log(searched_at DESC);
