package com.travelmap.api.review.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class RatingAggregateService {
    private final JdbcTemplate jdbc; private final ReindexService reindex;
    public RatingAggregateService(JdbcTemplate jdbc, ReindexService reindex) { this.jdbc = jdbc; this.reindex = reindex; }
    public void refresh(UUID poiId) {
        jdbc.update("""
            UPDATE poi SET avg_rating = COALESCE((SELECT AVG(rating) FROM review WHERE poi_id = ? AND status = 'PUBLISHED'), 0),
                           rating_count = (SELECT COUNT(*) FROM review WHERE poi_id = ? AND status = 'PUBLISHED'),
                           updated_at = CURRENT_TIMESTAMP
             WHERE id = ?
            """, poiId, poiId, poiId);
        reindex.reindex(poiId);
    }
}
