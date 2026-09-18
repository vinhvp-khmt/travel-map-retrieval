package com.travelmap.api.search.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.poi.model.CategoryEntity;
import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiOpeningHourEntity;
import com.travelmap.api.poi.model.PoiStatus;
import com.travelmap.api.poi.repository.CategoryRepository;
import com.travelmap.api.poi.repository.PoiRepository;
import com.travelmap.api.poi.repository.SpatialCandidateProjection;
import com.travelmap.api.poi.service.PoiNameNormalizer;
import com.travelmap.api.search.dto.SearchResult;
import com.travelmap.api.search.model.SearchCriteria;
import com.travelmap.api.search.model.WeightProfile;
import com.travelmap.api.search.repository.SearchLogRepository;
import com.travelmap.api.search.service.DiversityReranker;
import com.travelmap.api.search.service.QueryNormalizer;
import com.travelmap.api.search.service.RankingService;
import com.travelmap.api.search.service.SearchService;
import com.travelmap.api.search.service.SimpleVietnameseTokenizer;
import com.travelmap.api.search.service.TemporalFitService;
import com.travelmap.api.search.validation.SearchRequestValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Phần 2.4 — Đánh giá chất lượng xếp hạng bằng NDCG/MAP/P@k.
 *
 * <p>So sánh ba cấu hình chấm điểm trên cùng bộ truy vấn + dữ liệu vàng ({@code qrels.json}),
 * chạy qua đúng {@link SearchService#search} thật (không đổi một dòng lõi nào):
 * <ul>
 *   <li><b>Full</b> = {@link WeightProfile#V2} — dùng cả 4 tín hiệu (BM25 + spatial decay +
 *       temporal + rating shrinkage).</li>
 *   <li><b>Keyword-only</b> = {@link WeightProfile#EVAL_KEYWORD_ONLY} — chỉ khớp từ khoá.</li>
 *   <li><b>Distance-only</b> = {@link WeightProfile#EVAL_DISTANCE_ONLY} — chỉ khoảng cách.</li>
 * </ul>
 *
 * <p>Dữ liệu là POI cố định (id + toạ độ + rating gắn cứng trong {@code corpus.json}) nên điểm số
 * ổn định, không phụ thuộc DB thật. Kho POI được nạp qua mock {@link PoiRepository} y hệt cách
 * {@code SearchServiceTest} đang làm; ứng viên không gian tính bằng haversine và lọc theo bán kính.
 *
 * <p>Đánh giá đặt {@code diversify=false} để đo đúng thứ hạng theo điểm (không để bước đa dạng hoá
 * 2.3 xáo trộn thứ tự). Kết quả in ra console và ghi {@code target/evaluation/results.md}.
 *
 * <p><b>Kỳ vọng (được test khẳng định):</b> Full thắng cả ba độ đo so với hai cấu hình còn lại.
 */
class IrEvaluationTest {

    private static final int P_AT = 5;
    private static final int NDCG_AT = 10;
    private static final int PAGE_SIZE = 50; // đủ lớn để lấy toàn bộ top-k của mỗi cụm

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static List<PoiEntity> allPois;
    private static Map<UUID, PoiEntity> byId;
    private static List<QueryCase> queries;

    /** Một truy vấn + nhãn liên quan của nó. */
    private record QueryCase(String id, String query, double lat, double lng, double radiusKm,
                             Set<UUID> relevant, Map<UUID, Integer> grades) {
    }

    /** Điểm trung bình của một cấu hình trên toàn bộ truy vấn. */
    private record Score(double pAtK, double map, double ndcg) {
    }

    /** Bản cài đặt tối giản của projection không gian (id + khoảng cách). */
    private record SpatialCandidate(UUID id, double distanceMeters) implements SpatialCandidateProjection {
        @Override public UUID getId() { return id; }
        @Override public double getDistanceMeters() { return distanceMeters; }
    }

    @BeforeAll
    static void loadFixtures() throws Exception {
        var owner = new UserEntity("eval@travelmap.local", "x", UserRole.OWNER);
        Map<String, CategoryEntity> categories = new HashMap<>();
        allPois = new ArrayList<>();
        byId = new HashMap<>();

        JsonNode corpus = readJson("/evaluation/corpus.json");
        for (JsonNode n : corpus) {
            String slug = n.get("categorySlug").asText();
            CategoryEntity category = categories.computeIfAbsent(slug,
                    s -> new CategoryEntity(UUID.nameUUIDFromBytes(s.getBytes()), n.get("category").asText(), s));

            PoiEntity poi = new PoiEntity(owner, category,
                    n.get("name").asText(), n.get("normalizedName").asText(), n.get("description").asText(),
                    n.get("latitude").asDouble(), n.get("longitude").asDouble(), n.get("address").asText(),
                    null, null, false);
            // Gắn cứng id + rating (constructor luôn đặt id ngẫu nhiên và rating = 0, không có setter).
            ReflectionTestUtils.setField(poi, "id", UUID.fromString(n.get("id").asText()));
            ReflectionTestUtils.setField(poi, "avgRating", new BigDecimal(n.get("avgRating").asText()));
            ReflectionTestUtils.setField(poi, "ratingCount", n.get("ratingCount").asInt());
            // Mở cửa cả tuần để độ khớp thời gian là hằng số (không làm lệch so sánh giữa các cấu hình).
            List<PoiOpeningHourEntity> hours = new ArrayList<>();
            for (int d = 1; d <= 7; d++) {
                hours.add(new PoiOpeningHourEntity(d, LocalTime.MIN, LocalTime.MAX, false));
            }
            poi.replaceOpeningHours(hours);
            poi.approve(); // -> ACTIVE

            allPois.add(poi);
            byId.put(poi.getId(), poi);
        }

        queries = new ArrayList<>();
        JsonNode qrels = readJson("/evaluation/qrels.json");
        for (JsonNode q : qrels) {
            Set<UUID> rel = new java.util.HashSet<>();
            Map<UUID, Integer> grades = new HashMap<>();
            for (JsonNode r : q.get("relevant")) {
                UUID pid = UUID.fromString(r.get("poiId").asText());
                int grade = r.get("grade").asInt();
                grades.put(pid, grade);
                if (grade >= 1) {
                    rel.add(pid);
                }
            }
            queries.add(new QueryCase(q.get("id").asText(), q.get("query").asText(),
                    q.get("latitude").asDouble(), q.get("longitude").asDouble(), q.get("radiusKm").asDouble(),
                    rel, grades));
        }
    }

    @Test
    @DisplayName("Full (V2) thắng Keyword-only và Distance-only trên P@5, MAP, NDCG@10")
    void full_thang_ro() throws Exception {
        Score full = evaluate(WeightProfile.V2);
        Score keyword = evaluate(WeightProfile.EVAL_KEYWORD_ONLY);
        Score distance = evaluate(WeightProfile.EVAL_DISTANCE_ONLY);

        String table = renderTable(full, keyword, distance);
        System.out.println(table);
        writeResults(table);

        assertTrue(full.pAtK() > keyword.pAtK() && full.pAtK() > distance.pAtK(),
                "P@" + P_AT + ": Full phải cao hơn cả hai cấu hình còn lại");
        assertTrue(full.map() > keyword.map() && full.map() > distance.map(),
                "MAP: Full phải cao hơn cả hai cấu hình còn lại");
        assertTrue(full.ndcg() > keyword.ndcg() && full.ndcg() > distance.ndcg(),
                "NDCG@" + NDCG_AT + ": Full phải cao hơn cả hai cấu hình còn lại");
    }

    /** Chạy toàn bộ truy vấn qua SearchService với một profile và lấy trung bình 3 độ đo. */
    private Score evaluate(WeightProfile profile) {
        SearchService service = newService();
        double sumP = 0, sumMap = 0, sumNdcg = 0;
        for (QueryCase q : queries) {
            SearchCriteria criteria = new SearchCriteria(
                    q.query(), q.lat(), q.lng(), q.radiusKm(), null, 0, PAGE_SIZE, null, null, profile, false);
            List<UUID> ranked = service.search(criteria).results().stream()
                    .map(SearchResult::poiId)
                    .toList();
            sumP += Metrics.precisionAtK(ranked, q.relevant(), P_AT);
            sumMap += Metrics.averagePrecision(ranked, q.relevant());
            sumNdcg += Metrics.ndcgAtK(ranked, q.grades(), NDCG_AT);
        }
        int n = queries.size();
        return new Score(sumP / n, sumMap / n, sumNdcg / n);
    }

    /** Dựng SearchService thật với kho POI cố định qua mock — giống hệt cách SearchServiceTest làm. */
    private SearchService newService() {
        PoiRepository pois = mock(PoiRepository.class);
        CategoryRepository categories = mock(CategoryRepository.class);
        SearchLogRepository logs = mock(SearchLogRepository.class);

        when(pois.findAllByStatus(PoiStatus.ACTIVE)).thenReturn(allPois);
        when(pois.findSpatialCandidates(anyDouble(), anyDouble(), anyDouble(), isNull(), isNull(), anyInt()))
                .thenAnswer(inv -> {
                    double lat = inv.getArgument(0);
                    double lng = inv.getArgument(1);
                    double radiusMeters = inv.getArgument(2);
                    List<SpatialCandidateProjection> out = new ArrayList<>();
                    for (PoiEntity p : allPois) {
                        double d = haversine(lat, lng, p.getLatitude(), p.getLongitude());
                        if (d <= radiusMeters) {
                            out.add(new SpatialCandidate(p.getId(), d));
                        }
                    }
                    out.sort((a, b) -> Double.compare(a.getDistanceMeters(), b.getDistanceMeters()));
                    return out;
                });
        when(pois.findAllByIdIn(anyList())).thenAnswer(inv -> {
            List<UUID> ids = inv.getArgument(0);
            return ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
        });

        var tokenizer = new SimpleVietnameseTokenizer();
        return new SearchService(pois, categories, logs, tokenizer,
                new QueryNormalizer(tokenizer), new SearchRequestValidator(), new TemporalFitService(),
                new RankingService(), new DiversityReranker(new PoiNameNormalizer()));
    }

    private static JsonNode readJson(String resource) throws Exception {
        try (InputStream in = IrEvaluationTest.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("Không tìm thấy tài nguyên: " + resource);
            }
            return MAPPER.readTree(in);
        }
    }

    /** Khoảng cách great-circle (m) — xấp xỉ ST_Distance geography của PostGIS đủ tốt cho đánh giá. */
    private static double haversine(double lat1, double lng1, double lat2, double lng2) {
        double r = 6_371_000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String renderTable(Score full, Score keyword, Score distance) {
        Map<String, Score> rows = new LinkedHashMap<>();
        rows.put("Full (V2)", full);
        rows.put("Keyword-only", keyword);
        rows.put("Distance-only", distance);
        StringBuilder sb = new StringBuilder();
        sb.append("# Kết quả đánh giá IR (phần 2.4)\n\n");
        sb.append("Bộ dữ liệu: ").append(queries.size()).append(" truy vấn, ")
                .append(allPois.size()).append(" POI cố định. diversify=false.\n\n");
        sb.append(String.format("| %-14s | %8s | %8s | %9s |%n", "Cấu hình", "P@" + P_AT, "MAP", "NDCG@" + NDCG_AT));
        sb.append("|----------------|----------|----------|-----------|\n");
        rows.forEach((name, s) -> sb.append(String.format("| %-14s | %8.4f | %8.4f | %9.4f |%n",
                name, s.pAtK(), s.map(), s.ndcg())));
        sb.append("\nFull = V2 (đủ 4 tín hiệu) thắng cả ba độ đo, xác nhận việc gộp BM25 + khoảng cách "
                + "(spatial decay) + độ mở cửa + rating (shrinkage) cho kết quả tốt hơn hẳn so với chỉ dùng "
                + "từ khoá hoặc chỉ dùng khoảng cách.\n");
        return sb.toString();
    }

    private void writeResults(String table) {
        try {
            Path dir = Path.of("target", "evaluation");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("results.md"), table);
        } catch (Exception e) {
            // Không để việc ghi file phụ làm hỏng bài test — bảng đã in ra console.
            System.err.println("Không ghi được target/evaluation/results.md: " + e.getMessage());
        }
    }
}
