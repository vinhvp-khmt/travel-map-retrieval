CREATE TABLE booking (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id),
    poi_id UUID NOT NULL REFERENCES poi(id),
    visit_at TIMESTAMPTZ NOT NULL,
    slot_end_at TIMESTAMPTZ NOT NULL,
    party_size INTEGER NOT NULL CHECK (party_size > 0),
    notes VARCHAR(1000),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'EXPIRED')),
    hold_expires_at TIMESTAMPTZ,
    deposit_amount NUMERIC(12,2) NOT NULL CHECK (deposit_amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CHECK (slot_end_at > visit_at)
);

CREATE INDEX idx_booking_poi_slot ON booking(poi_id, visit_at, slot_end_at);
CREATE INDEX idx_booking_active_hold ON booking(hold_expires_at)
    WHERE status = 'PENDING';
CREATE INDEX idx_booking_user_created ON booking(user_id, created_at DESC);

CREATE TABLE payment (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE REFERENCES booking(id),
    gateway VARCHAR(30) NOT NULL,
    external_txn_id VARCHAR(120) UNIQUE,
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    amount NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL CHECK (status IN ('CREATED', 'PAID', 'FAILED', 'REFUNDED')),
    payment_url VARCHAR(500),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE payment_webhook_event (
    id UUID PRIMARY KEY,
    gateway VARCHAR(30) NOT NULL,
    event_id VARCHAR(120) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (gateway, event_id)
);
