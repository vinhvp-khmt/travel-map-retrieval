package com.travelmap.api.search.service;

import com.travelmap.api.search.dto.ScoreDetail;
import org.springframework.stereotype.Service;

@Service
public class RankingService {
    public ScoreDetail score(double rawBm25, double maxBm25, double distanceMeters,
                             double radiusMeters, double temporalFit, double averageRating) {
        double bm25 = maxBm25 <= 0 ? 0 : clamp(rawBm25 / maxBm25);
        double spatial = clamp(1.0 - distanceMeters / radiusMeters);
        double temporal = clamp(temporalFit);
        double rating = clamp(averageRating / 5.0);
        double finalScore = 0.40 * bm25 + 0.30 * spatial + 0.20 * temporal + 0.10 * rating;
        return new ScoreDetail(round(bm25), round(spatial), round(temporal), round(rating), round(finalScore));
    }

    private static double clamp(double value) { return Math.max(0, Math.min(1, value)); }
    private static double round(double value) { return Math.round(value * 10_000.0) / 10_000.0; }
}
