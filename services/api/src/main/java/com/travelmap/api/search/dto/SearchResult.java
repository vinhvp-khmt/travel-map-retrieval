package com.travelmap.api.search.dto;

import java.util.UUID;

/** {@code distanceMeters} là {@code null} khi request search không có GPS (phần 2.4). */
public record SearchResult(
        UUID poiId,
        String name,
        String category,
        String address,
        double latitude,
        double longitude,
        Double distanceMeters,
        boolean open,
        ScoreDetail scoreDetail
) { }
