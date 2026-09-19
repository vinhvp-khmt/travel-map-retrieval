package com.travelmap.api.search.history;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Truy cập bảng {@code search_history} (V9) qua JdbcTemplate, cùng phong cách với
 * {@link com.travelmap.api.search.repository.SearchLogRepository} — bảng anh em, nhưng
 * gắn với user thay vì ẩn danh, và UPSERT theo (user_id, normalized_query) để danh sách
 * "gần đây" không lặp lại truy vấn giống nhau.
 */
@Repository
public class SearchHistoryRepository {
    private static final int DEFAULT_LIMIT = 10;

    private final JdbcTemplate jdbcTemplate;

    public SearchHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Ghi một lượt search vào lịch sử; lặp lại cùng normalizedQuery thì cập nhật lên đầu. */
    public void record(UUID userId, String queryText, String normalizedQuery, double latitude,
                       double longitude, double radiusKm, int resultCount) {
        jdbcTemplate.update("""
                INSERT INTO search_history
                    (id, user_id, query_text, normalized_query, latitude, longitude, radius_km, result_count, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
                ON CONFLICT (user_id, normalized_query) DO UPDATE SET
                    query_text = EXCLUDED.query_text, latitude = EXCLUDED.latitude,
                    longitude = EXCLUDED.longitude, radius_km = EXCLUDED.radius_km,
                    result_count = EXCLUDED.result_count, created_at = NOW()
                """, UUID.randomUUID(), userId, queryText.trim(), normalizedQuery, latitude, longitude,
                radiusKm, resultCount);
    }

    public List<SearchHistoryItem> list(UUID userId, int limit) {
        int size = limit <= 0 ? DEFAULT_LIMIT : limit;
        return jdbcTemplate.query("""
                SELECT id, query_text, latitude, longitude, radius_km, result_count, created_at
                FROM search_history WHERE user_id = ? ORDER BY created_at DESC LIMIT ?
                """, (rs, rowNum) -> new SearchHistoryItem(
                        UUID.fromString(rs.getString("id")), rs.getString("query_text"),
                        rs.getDouble("latitude"), rs.getDouble("longitude"), rs.getDouble("radius_km"),
                        rs.getInt("result_count"), toInstant(rs.getTimestamp("created_at"))),
                userId, size);
    }

    public void deleteAll(UUID userId) {
        jdbcTemplate.update("DELETE FROM search_history WHERE user_id = ?", userId);
    }

    public void delete(UUID userId, UUID historyId) {
        jdbcTemplate.update("DELETE FROM search_history WHERE user_id = ? AND id = ?", userId, historyId);
    }

    private static java.time.Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
