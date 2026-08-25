package com.travelmap.api.search.dto;

import java.util.UUID;

public record SearchResult(
        UUID poiId,
        String name,
        String category,
        String address,
        double latitude,
        double longitude,
        double distanceMeters,
        boolean open,
        ScoreDetail scoreDetail
) { }
