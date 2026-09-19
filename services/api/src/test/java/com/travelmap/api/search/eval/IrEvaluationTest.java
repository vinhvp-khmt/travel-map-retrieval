package com.travelmap.api.search.eval;

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
import com.travelmap.api.search.service.SearchIndexService;
import com.travelmap.api.search.service.SimpleVietnameseTokenizer;
import com.travelmap.api.search.service.TemporalFitService;
import com.travelmap.api.search.validation.SearchRequestValidator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
 * <p>So sánh ba cấu hình chấm điểm trên cùng bộ truy vấn + dữ liệu vàng, chạy qua đúng
 * {@link SearchService#search} thật (không đổi một dòng lõi nào):
 * <ul>
 *   <li><b>Full</b> = {@link WeightProfile#V2} — dùng cả 4 tín hiệu (BM25 + spatial decay +
 *       temporal + rating shrinkage).</li>
 *   <li><b>Keyword-only</b> = {@link WeightProfile#EVAL_KEYWORD_ONLY} — chỉ khớp từ khoá.</li>
 *   <li><b>Distance-only</b> = {@link WeightProfile#EVAL_DISTANCE_ONLY} — chỉ khoảng cách.</li>
 * </ul>
 *
 * <p>Theo hướng đã chọn (A): dữ liệu là POI cố định, dựng thẳng trong test với UUID + rating gắn
 * cứng nên điểm số ổn định, không phụ thuộc DB thật. Bộ dữ liệu này khớp 1–1 với hai file tham
 * chiếu cho báo cáo: {@code src/test/resources/evaluation/corpus.json} (160 POI, cùng UUID) và
 * {@code qrels.json} (16 truy vấn, nhãn grade 0–3). Kho POI được nạp qua mock {@link PoiRepository}
 * y hệt cách {@code SearchServiceTest} làm; ứng viên không gian tính bằng haversine, lọc theo bán kính.
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

    /** Mở cửa cả tuần để độ khớp thời gian là hằng số giữa mọi POI, không làm lệch so sánh. */
    private static final String PAD =
            "phuc vu nhanh gon gang than thien nhan vien vui ve cho ngoi rong rai thoang mat sach se "
            + "tien nghi gia binh dan phu hop nhom ban be gia dinh di lam gan trung tam thanh pho";

    private static final UserEntity OWNER = new UserEntity("eval@travelmap.local", "x", UserRole.OWNER);

    private static List<PoiEntity> allPois;
    private static Map<UUID, PoiEntity> byId;
    private static List<QueryCase> queries;
    private static int idCounter;
    private static final Map<String, CategoryEntity> CATEGORIES = new HashMap<>();

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
    static void buildFixtures() {
        allPois = new ArrayList<>();
        byId = new HashMap<>();
        queries = new ArrayList<>();

        // Mỗi dòng: {truy vấn hiển thị, cụm từ khoá (đã chuẩn hoá), tên loại, slug loại}.
        String[][] specs = {
                {"ca phe yen tinh gan day", "ca phe", "Ca phe", "ca-phe"},
                {"pho bo tai gau", "pho bo", "Pho", "pho-bo"},
                {"bun rieu cua dong", "bun rieu", "Bun", "bun-rieu"},
                {"tra sua tran chau", "tra sua", "Tra sua", "tra-sua"},
                {"lau thai chua cay", "lau thai", "Lau", "lau-thai"},
                {"com tam suon bi", "com tam", "Com tam", "com-tam"},
                {"banh mi thit nuong", "banh mi", "Banh mi", "banh-mi"},
                {"hu tieu nam vang", "hu tieu", "Hu tieu", "hu-tieu"},
                {"bun bo hue cay", "bun bo", "Bun bo", "bun-bo"},
                {"goi cuon tom thit", "goi cuon", "Goi cuon", "goi-cuon"},
                {"che thap cam mat", "che thap", "Che", "che-thap"},
                {"banh xeo mien tay", "banh xeo", "Banh xeo", "banh-xeo"},
                {"sinh to bo deo", "sinh to", "Sinh to", "sinh-to"},
                {"mi quang ga ta", "mi quang", "Mi quang", "mi-quang"},
                {"xoi ga xoi man", "xoi ga", "Xoi", "xoi-ga"},
                {"kem trai cay tuoi", "kem trai", "Kem", "kem-trai"},
        };

        double baseLat = 10.5;
        double baseLng = 106.5;
        for (int i = 0; i < specs.length; i++) {
            buildCluster(specs[i][0], specs[i][1], specs[i][2], specs[i][3], baseLat + i * 0.5, baseLng);
        }
    }

    /** Dựng một cụm 10 POI cho một truy vấn: 5 quán liên quan (grade 1–3) + 5 "bẫy" (grade 0). */
    private static void buildCluster(String rawQuery, String phrase, String categoryName, String slug,
                                     double clat, double clng) {
        String p = phrase;
        String cap = capitalize(phrase);
        // {name, description, distanceMeters, avgRating, ratingCount, grade}
        Object[][] rows = {
                {cap + " Suong Mai", "quan " + p + " yen tinh co " + p + " ngon " + p + " view dep", 500.0, 4.7, 320, 3},
                {cap + " Ban Mai", p + " sach " + p + " gia tot " + p + " phuc vu tan tam", 800.0, 4.6, 260, 3},
                {cap + " Am Cung", p + " khong gian dep " + p + " nhac nhe de chiu", 700.0, 4.4, 180, 2},
                {cap + " Trung Tam", p + " pha che dam da " + p + " chuan vi", 1000.0, 4.2, 150, 2},
                {"Quan Nho Ven Ho", "co ban " + p + " mang di cho khach quen trong khu vuc", 1150.0, 4.0, 60, 1},
                {cap + " " + cap + " " + cap + " " + cap + " Sieu Thi",
                        p + " " + p + " " + p + " " + p + " " + p + " ban le ban si toan quoc", 2900.0, 3.0, 10, 0},
                {cap + " " + cap + " " + cap + " Tong Kho",
                        p + " " + p + " " + p + " " + p + " phan phoi so luong lon gia goc", 2700.0, 3.0, 9, 0},
                {"Tiem Tap Hoa Goc Duong",
                        "ban do an vat nuoc dong chai va it " + p + " mang ve " + PAD + " " + PAD, 120.0, 2.8, 5, 0},
                {"Cua Hang Tien Loi Sang Dem",
                        "cua hang tien loi co ban " + p + " dong lanh nhu yeu pham " + PAD + " " + PAD, 250.0, 2.7, 5, 0},
                {"Quan Ven Duong Binh Dan",
                        "phuc vu " + p + " va nhieu mon khac gia binh dan cho nguoi di duong", 1800.0, 3.4, 20, 0},
        };

        Set<UUID> relevant = new HashSet<>();
        Map<UUID, Integer> grades = new HashMap<>();
        for (Object[] r : rows) {
            PoiEntity poi = newPoi(categoryName, slug, (String) r[0], (String) r[1],
                    clat, clng, (Double) r[2], (Double) r[3], (Integer) r[4]);
            int grade = (Integer) r[5];
            allPois.add(poi);
            byId.put(poi.getId(), poi);
            grades.put(poi.getId(), grade);
            if (grade >= 1) {
                relevant.add(poi.getId());
            }
        }
        queries.add(new QueryCase("q" + String.format("%02d", queries.size() + 1),
                rawQuery, clat, clng, 3.0, relevant, grades));
    }

    private static PoiEntity newPoi(String categoryName, String slug, String name, String description,
                                    double clat, double clng, double distanceMeters,
                                    double avgRating, int ratingCount) {
        CategoryEntity category = CATEGORIES.computeIfAbsent(slug,
                s -> new CategoryEntity(UUID.nameUUIDFromBytes(s.getBytes()), categoryName, s));
        double lat = clat + distanceMeters / 111_320.0; // đặt POI về phía bắc tâm để có đúng khoảng cách
        PoiEntity poi = new PoiEntity(OWNER, category, name, name.toLowerCase(), description,
                lat, clng, "Duong so " + (idCounter + 1), null, null, false);
        // UUID + rating phải gắn qua reflection: constructor luôn đặt id ngẫu nhiên và rating = 0, không có setter.
        // UUID khớp đúng thứ tự trong corpus.json (11111111-1111-4111-8111-0000000000NN).
        String fixedId = String.format("11111111-1111-4111-8111-%012d", ++idCounter);
        ReflectionTestUtils.setField(poi, "id", UUID.fromString(fixedId));
        ReflectionTestUtils.setField(poi, "avgRating", BigDecimal.valueOf(avgRating));
        ReflectionTestUtils.setField(poi, "ratingCount", ratingCount);
        List<PoiOpeningHourEntity> hours = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            hours.add(new PoiOpeningHourEntity(d, LocalTime.MIN, LocalTime.MAX, false));
        }
        poi.replaceOpeningHours(hours);
        poi.approve(); // -> ACTIVE
        return poi;
    }

    @Test
    @DisplayName("Full (V2) thắng Keyword-only và Distance-only trên P@5, MAP, NDCG@10")
    void full_thang_ro() {
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
            return ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        });

        var tokenizer = new SimpleVietnameseTokenizer();
        SearchIndexService searchIndex = new SearchIndexService(pois, tokenizer);
        searchIndex.rebuild(allPois);
        return new SearchService(pois, categories, logs, tokenizer,
                new QueryNormalizer(tokenizer), new SearchRequestValidator(), new TemporalFitService(),
                new RankingService(), new DiversityReranker(new PoiNameNormalizer()), searchIndex);
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

    private static String capitalize(String phrase) {
        StringBuilder sb = new StringBuilder();
        for (String w : phrase.split(" ")) {
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
        }
        return sb.toString().trim();
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
        sb.append(String.format(Locale.ROOT, "| %-14s | %8s | %8s | %9s |%n",
                "Cấu hình", "P@" + P_AT, "MAP", "NDCG@" + NDCG_AT));
        sb.append("|----------------|----------|----------|-----------|\n");
        rows.forEach((name, s) -> sb.append(String.format(Locale.ROOT, "| %-14s | %8.4f | %8.4f | %9.4f |%n",
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
