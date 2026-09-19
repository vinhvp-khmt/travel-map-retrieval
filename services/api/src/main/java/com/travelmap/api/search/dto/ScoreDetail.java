package com.travelmap.api.search.dto;

public record ScoreDetail(
        double bm25,
        double spatial,
        double temporal,
        double rating,
        double finalScore,
        String rankingProfile,
        double rawBm25,
        double distanceMeters,
        double averageRating,
        long ratingCount,
        double bm25Weight,
        double spatialWeight,
        double temporalWeight,
        double ratingWeight,
        double bm25Contribution,
        double spatialContribution,
        double temporalContribution,
        double ratingContribution
) {
    /** Backwards-compatible constructor for fixtures that only need normalized scores. */
    public ScoreDetail(double bm25, double spatial, double temporal, double rating, double finalScore) {
        this(bm25, spatial, temporal, rating, finalScore, "unknown", 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0);
    }
}
