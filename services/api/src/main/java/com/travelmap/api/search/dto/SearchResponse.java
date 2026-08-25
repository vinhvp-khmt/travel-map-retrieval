package com.travelmap.api.search.dto;

import java.util.List;

public record SearchResponse(
        String normalizedQuery,
        int page,
        int size,
        int total,
        List<SearchResult> results,
        String suggestion
) { }
