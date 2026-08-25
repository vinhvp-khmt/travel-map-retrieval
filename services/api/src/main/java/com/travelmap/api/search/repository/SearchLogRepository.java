package com.travelmap.api.search.repository;

import com.travelmap.api.search.model.SearchCriteria;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class SearchLogRepository {
    private final JdbcTemplate jdbcTemplate;

    public SearchLogRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void save(SearchCriteria criteria, String normalizedQuery, int resultCount) {
        jdbcTemplate.update("""
                INSERT INTO search_log
                    (id, query_text, normalized_query, latitude, longitude, radius_km, result_count)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), criteria.query().trim(), normalizedQuery, criteria.latitude(),
                criteria.longitude(), criteria.radiusKm(), resultCount);
    }
}
