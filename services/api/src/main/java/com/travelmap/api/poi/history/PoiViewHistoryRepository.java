package com.travelmap.api.poi.history;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Truy cập bảng {@code poi_view_history} (V9) qua JdbcTemplate. UPSERT theo
 * (user_id, poi_id): xem lại một POI đã xem trước đó chỉ đẩy nó lên đầu danh sách thay vì
 * tạo dòng trùng.
 *
 * <p>{@link #list} JOIN sang {@code poi}/{@code category} để lấy đủ dữ liệu hiển thị
 * (tên, địa chỉ, rating…) mà không cần thêm lượt gọi API nào ở frontend, và chỉ lấy POI
 * còn {@code ACTIVE} — POI bị gỡ/từ chối sau khi user đã xem thì không hiện lại nữa.
 */
@Repository
public class PoiViewHistoryRepository {
    private static final int DEFAULT_LIMIT = 10;

    private final JdbcTemplate jdbcTemplate;

    public PoiViewHistoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Ghi một lượt xem POI; xem lại cùng POI thì cập nhật viewed_at lên đầu. */
    public void record(UUID userId, UUID poiId) {
        jdbcTemplate.update("""
                INSERT INTO poi_view_history (id, user_id, poi_id, viewed_at)
                VALUES (?, ?, ?, NOW())
                ON CONFLICT (user_id, poi_id) DO UPDATE SET viewed_at = NOW()
                """, UUID.randomUUID(), userId, poiId);
    }

    public List<ViewedPoiItem> list(UUID userId, int limit) {
        int size = limit <= 0 ? DEFAULT_LIMIT : limit;
        return jdbcTemplate.query("""
                SELECT p.id, p.name, c.name AS category_name, p.address, p.latitude, p.longitude,
                       p.avg_rating, p.rating_count, h.viewed_at
                FROM poi_view_history h
                JOIN poi p ON p.id = h.poi_id AND p.status = 'ACTIVE'
                JOIN category c ON c.id = p.category_id
                WHERE h.user_id = ?
                ORDER BY h.viewed_at DESC LIMIT ?
                """, (rs, rowNum) -> new ViewedPoiItem(
                        UUID.fromString(rs.getString("id")), rs.getString("name"), rs.getString("category_name"),
                        rs.getString("address"), rs.getDouble("latitude"), rs.getDouble("longitude"),
                        rs.getDouble("avg_rating"), rs.getInt("rating_count"), toInstant(rs.getTimestamp("viewed_at"))),
                userId, size);
    }

    public void deleteAll(UUID userId) {
        jdbcTemplate.update("DELETE FROM poi_view_history WHERE user_id = ?", userId);
    }

    public void delete(UUID userId, UUID poiId) {
        jdbcTemplate.update("DELETE FROM poi_view_history WHERE user_id = ? AND poi_id = ?", userId, poiId);
    }

    private static java.time.Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
