package com.travelmap.api.search.history;

import java.time.Instant;
import java.util.UUID;

/** Một mục "tìm kiếm gần đây" của user, dùng cho GET /api/v1/search/history. */
public record SearchHistoryItem(
        UUID id,
        String query,
        double latitude,
        double longitude,
        double radiusKm,
        int resultCount,
        Instant createdAt
) { }
