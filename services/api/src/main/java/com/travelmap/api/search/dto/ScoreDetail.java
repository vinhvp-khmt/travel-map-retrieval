package com.travelmap.api.search.dto;

public record ScoreDetail(
        double bm25,
        double spatial,
        double temporal,
        double rating,
        double finalScore
) { }
