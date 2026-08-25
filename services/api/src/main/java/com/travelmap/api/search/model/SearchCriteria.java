package com.travelmap.api.search.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SearchCriteria(
        String query,
        double latitude,
        double longitude,
        double radiusKm,
        OffsetDateTime visitAt,
        int page,
        int size,
        Integer priceLevel,
        UUID categoryId
) { }
