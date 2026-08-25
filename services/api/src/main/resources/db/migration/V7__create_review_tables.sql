CREATE TABLE review (
    id UUID PRIMARY KEY,
    booking_id UUID NOT NULL UNIQUE REFERENCES booking(id),
    poi_id UUID NOT NULL REFERENCES poi(id),
    user_id UUID NOT NULL REFERENCES app_user(id),
    rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
    comment VARCHAR(2000),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PUBLISHED', 'HIDDEN', 'DELETED')),
    editable_until TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_review_poi_status ON review(poi_id, status);
CREATE INDEX idx_review_user ON review(user_id);

CREATE TABLE review_image (
    id UUID PRIMARY KEY,
    review_id UUID NOT NULL REFERENCES review(id) ON DELETE CASCADE,
    url VARCHAR(1000) NOT NULL,
    sort_order INTEGER NOT NULL CHECK (sort_order BETWEEN 0 AND 4),
    UNIQUE(review_id, sort_order)
);
