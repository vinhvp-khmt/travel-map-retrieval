package com.travelmap.api.report.service;

import com.travelmap.api.report.dto.ReportOverview;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class ReportService {
    private final JdbcTemplate jdbc;
    public ReportService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public ReportOverview ownerOverview(String email) {
        return query("WHERE p.owner_id = (SELECT id FROM app_user WHERE lower(email) = lower(?))", email);
    }
    public ReportOverview adminOverview() { return query("", new Object[0]); }

    private ReportOverview query(String scope, Object... args) {
        String poiScope = "SELECT p.id FROM poi p " + scope;
        String sql = """
            WITH scoped_poi AS (%s), metrics AS (
              SELECT
                (SELECT COUNT(*) FROM scoped_poi) poi_count,
                (SELECT COUNT(*) FROM booking b WHERE b.poi_id IN (SELECT id FROM scoped_poi)) booking_count,
                (SELECT COUNT(*) FROM booking b WHERE b.poi_id IN (SELECT id FROM scoped_poi) AND b.status = 'COMPLETED') completed_count,
                (SELECT COALESCE(SUM(pay.amount), 0) FROM payment pay JOIN booking b ON b.id = pay.booking_id
                   WHERE b.poi_id IN (SELECT id FROM scoped_poi) AND pay.status = 'PAID') paid_revenue,
                (SELECT COUNT(*) FROM review r WHERE r.poi_id IN (SELECT id FROM scoped_poi) AND r.status = 'PUBLISHED') review_count,
                (SELECT COALESCE(AVG(r.rating), 0) FROM review r WHERE r.poi_id IN (SELECT id FROM scoped_poi) AND r.status = 'PUBLISHED') avg_rating
            ) SELECT *, CASE WHEN booking_count = 0 THEN 0
                        ELSE ROUND(completed_count * 100.0 / booking_count, 2) END conversion_rate FROM metrics
            """.formatted(poiScope);
        return jdbc.queryForObject(sql, (rs, row) -> new ReportOverview(
                rs.getLong("poi_count"), rs.getLong("booking_count"), rs.getLong("completed_count"),
                rs.getBigDecimal("conversion_rate"), rs.getBigDecimal("paid_revenue"),
                rs.getLong("review_count"), rs.getBigDecimal("avg_rating")), args);
    }
}
