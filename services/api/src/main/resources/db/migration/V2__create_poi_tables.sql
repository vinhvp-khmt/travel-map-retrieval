CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE category (
    id UUID PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    slug VARCHAR(80) NOT NULL UNIQUE
);

INSERT INTO category (id, name, slug) VALUES
    ('10000000-0000-0000-0000-000000000001', 'Cà phê', 'ca-phe'),
    ('10000000-0000-0000-0000-000000000002', 'Nhà hàng', 'nha-hang'),
    ('10000000-0000-0000-0000-000000000003', 'Tham quan', 'tham-quan'),
    ('10000000-0000-0000-0000-000000000004', 'Lưu trú', 'luu-tru');

CREATE TABLE poi (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES app_user(id),
    category_id UUID NOT NULL REFERENCES category(id),
    name VARCHAR(120) NOT NULL,
    normalized_name VARCHAR(120) NOT NULL,
    description VARCHAR(3000),
    latitude DOUBLE PRECISION NOT NULL CHECK (latitude BETWEEN -90 AND 90),
    longitude DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    location geography(Point, 4326) NOT NULL,
    address VARCHAR(500) NOT NULL,
    price_level SMALLINT CHECK (price_level BETWEEN 1 AND 4),
    capacity INTEGER CHECK (capacity > 0),
    booking_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING_APPROVAL', 'ACTIVE', 'REJECTED', 'INACTIVE')),
    avg_rating NUMERIC(3,2) NOT NULL DEFAULT 0,
    rating_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE OR REPLACE FUNCTION travelmap_set_poi_location() RETURNS trigger AS $$
BEGIN
    NEW.location = ST_SetSRID(ST_MakePoint(NEW.longitude, NEW.latitude), 4326)::geography;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_poi_location
    BEFORE INSERT OR UPDATE OF latitude, longitude ON poi
    FOR EACH ROW EXECUTE FUNCTION travelmap_set_poi_location();

CREATE INDEX idx_poi_active_location ON poi USING gist (location) WHERE status = 'ACTIVE';
CREATE INDEX idx_poi_owner ON poi(owner_id);
CREATE INDEX idx_poi_normalized_name ON poi(normalized_name);

CREATE TABLE poi_opening_hour (
    id UUID PRIMARY KEY,
    poi_id UUID NOT NULL REFERENCES poi(id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    open_time TIME,
    close_time TIME,
    closed BOOLEAN NOT NULL DEFAULT FALSE,
    spans_next_day BOOLEAN NOT NULL DEFAULT FALSE,
    CHECK ((closed = TRUE AND open_time IS NULL AND close_time IS NULL)
        OR (closed = FALSE AND open_time IS NOT NULL AND close_time IS NOT NULL))
);

ALTER TABLE poi_opening_hour ADD CONSTRAINT no_same_day_opening_overlap
    EXCLUDE USING gist (
        poi_id WITH =,
        day_of_week WITH =,
        int4range(
            EXTRACT(EPOCH FROM open_time)::INTEGER,
            EXTRACT(EPOCH FROM close_time)::INTEGER,
            '[)'
        ) WITH &&
    ) WHERE (closed = FALSE AND spans_next_day = FALSE);

CREATE TABLE poi_approval_history (
    id UUID PRIMARY KEY,
    poi_id UUID NOT NULL REFERENCES poi(id) ON DELETE CASCADE,
    admin_id UUID NOT NULL REFERENCES app_user(id),
    decision VARCHAR(20) NOT NULL CHECK (decision IN ('APPROVED', 'REJECTED')),
    reason VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_poi_approval_history_poi ON poi_approval_history(poi_id, created_at DESC);
