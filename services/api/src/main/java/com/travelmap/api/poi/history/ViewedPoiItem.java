package com.travelmap.api.poi.history;

import java.time.Instant;
import java.util.UUID;

/** Một POI mà user đã từng mở chi tiết, dùng cho GET /api/v1/users/me/viewed-pois. */
public record ViewedPoiItem(
        UUID poiId,
        String name,
        String category,
        String address,
        double latitude,
        double longitude,
        double avgRating,
        int ratingCount,
        Instant viewedAt
) { }
