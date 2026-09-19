-- Phase 6/7 (IR search plan): lich su search va POI da xem CUA TUNG USER.
--
-- Khac voi search_log (V3, an danh, moi lan search 1 dong, chi dung cho
-- analytics) — hai bang nay gan voi app_user, phuc vu UI "Tim kiem gan day"
-- va "Dia diem da xem". Dung UPSERT (UNIQUE + ON CONFLICT o tang service)
-- de moi truy van/POI chi xuat hien 1 lan trong danh sach, day len dau khi
-- lap lai — dung UX "recent" thong thuong, tranh danh sach bi nguoi dung
-- lap di lap lai lam ray.

CREATE TABLE search_history (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    query_text VARCHAR(200) NOT NULL,
    normalized_query VARCHAR(200) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    radius_km NUMERIC(5,2) NOT NULL,
    result_count INTEGER NOT NULL CHECK (result_count >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_search_history_user_query UNIQUE (user_id, normalized_query)
);

CREATE INDEX idx_search_history_user_created ON search_history(user_id, created_at DESC);

CREATE TABLE poi_view_history (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    poi_id UUID NOT NULL REFERENCES poi(id) ON DELETE CASCADE,
    viewed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_poi_view_history_user_poi UNIQUE (user_id, poi_id)
);

CREATE INDEX idx_poi_view_history_user_viewed ON poi_view_history(user_id, viewed_at DESC);
