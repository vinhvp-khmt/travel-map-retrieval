package com.travelmap.api.search.eval;

import com.travelmap.api.search.dto.SearchResponse;
import com.travelmap.api.search.dto.SearchResult;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Phase 5 (dataset/evaluation plan) — evaluation runner THẬT, không mock.
 *
 * <p>Khác với {@link IrEvaluationTest} (dùng kho POI tự dựng in-memory, chạy an toàn trong CI
 * mà không cần hạ tầng gì), lớp này gọi HTTP thật vào
 * {@code GET /api/v1/search} của một API đang chạy, đọc ground truth từ
 * {@code docs/qrels/ir_dataset_qrels.json} (không hard-code trong Java), và đo trên đúng
 * dữ liệu đã seed bằng {@code infra/seed/ir_dataset_seed.sql} (qua {@code ./infra/scripts/seed-demo.sh}).
 *
 * <p><b>Cách chạy (1 lệnh, đúng yêu cầu "npm run evaluate" tương đương của plan):</b>
 * <pre>
 *   colima start &amp;&amp; docker compose -f infra/docker-compose.yml up -d postgres
 *   ./infra/scripts/seed-demo.sh
 *   cd services/api &amp;&amp; ./mvnw -q clean package -DskipTests
 *   java -jar target/travelmap-api-*.jar &amp;   # API thật, cổng 8080
 *   ./mvnw test -Dtest=IrDatasetEvaluationRunner   # method-pattern *Test* nên KHÔNG chạy
 *                                                    # trong `mvn test` mặc định (không cần
 *                                                    # API sống mới pass được CI) — chỉ chạy
 *                                                    # khi gọi -Dtest= tường minh như trên.
 * </pre>
 *
 * <p>Không có API sống ở {@code localhost:8080} thì test tự {@code assume}-skip (không fail
 * CI), in rõ lý do ra console.
 *
 * <p>Kết quả: in bảng ra console + ghi {@code outputs/evaluation/results.json} và
 * {@code results.csv} (aggregate theo 3 rankingMode, và per-query breakdown) — dùng thẳng
 * cho report, không bịa số.
 */
class IrDatasetEvaluationRunner {

    private static final String API_BASE_URL = System.getProperty("evalApiBaseUrl", "http://localhost:8080");
    private static final Path QRELS_FILE = Path.of("..", "..", "docs", "qrels", "ir_dataset_qrels.json");
    private static final Path OUTPUT_DIR = Path.of("..", "..", "outputs", "evaluation");
    private static final int P_AT = 5;
    private static final int NDCG_AT = 10;
    private static final int RESULT_SIZE = 50;
    private static final List<String> MODES = List.of("keyword", "distance", "full");

    private final ObjectMapper mapper = JsonMapper.builder().build();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();

    private record QueryEntry(String queryId, String query, Map<String, Integer> relevance) { }

    private record QrelsFile(double referenceLatitude, double referenceLongitude, double radiusKm,
                             String visitAt, List<QueryEntry> queries) { }

    private record PerQueryResult(String queryId, String query, String mode,
                                  double pAtK, double ap, double ndcg) { }

    @Test
    void runEvaluation() throws IOException, InterruptedException {
        assumeTrue(isApiUp(), () -> "API khong chay o " + API_BASE_URL
                + " -> bo qua (khong fail CI). Xem javadoc lop nay de biet cach chay evaluation that.");

        QrelsFile qrels = loadQrels();
        assertTrue(!qrels.queries().isEmpty(), "qrels rong — kiem tra docs/qrels/ir_dataset_qrels.json");

        List<PerQueryResult> perQuery = new ArrayList<>();
        for (QueryEntry q : qrels.queries()) {
            var rel = q.relevance().entrySet().stream()
                    .filter(e -> e.getValue() >= 1)
                    .map(e -> UUID.fromString(e.getKey()))
                    .collect(java.util.stream.Collectors.toSet());
            Map<UUID, Integer> grades = new LinkedHashMap<>();
            q.relevance().forEach((id, grade) -> grades.put(UUID.fromString(id), grade));

            for (String mode : MODES) {
                List<UUID> ranked = search(q.query(), qrels.referenceLatitude(), qrels.referenceLongitude(),
                        qrels.radiusKm(), qrels.visitAt(), mode);
                double p = Metrics.precisionAtK(ranked, rel, P_AT);
                double ap = Metrics.averagePrecision(ranked, rel);
                double ndcg = Metrics.ndcgAtK(ranked, grades, NDCG_AT);
                perQuery.add(new PerQueryResult(q.queryId(), q.query(), mode, p, ap, ndcg));
            }
        }

        Map<String, double[]> aggregate = aggregate(perQuery);
        String table = renderReport(qrels.queries().size(), aggregate, perQuery);
        System.out.println(table);
        writeOutputs(aggregate, perQuery);

        // Bat bien on dinh nhat, khong phu thuoc vao cach cham diem qrels: bo qua toan bo
        // tin hieu van ban (distance-only) phai kem han hai cau hinh con lai tren MAP —
        // vi no khong biet gi ve noi dung truy van.
        assertTrue(aggregate.get("distance")[1] < aggregate.get("keyword")[1]
                        && aggregate.get("distance")[1] < aggregate.get("full")[1],
                "Distance-only (bo qua van ban) phai co MAP thap hon ro rang so voi hai cau hinh con lai");
    }

    private boolean isApiUp() {
        try {
            HttpResponse<Void> response = http.send(
                    HttpRequest.newBuilder(URI.create(API_BASE_URL + "/api/v1/health"))
                            .timeout(Duration.ofSeconds(2)).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200;
        } catch (Exception exception) {
            return false;
        }
    }

    private QrelsFile loadQrels() throws IOException {
        byte[] bytes = Files.readAllBytes(QRELS_FILE);
        var root = mapper.readTree(bytes);
        List<QueryEntry> queries = new ArrayList<>();
        for (var node : root.get("queries")) {
            Map<String, Integer> relevance = new LinkedHashMap<>();
            node.get("relevance").properties().forEach(e -> relevance.put(e.getKey(), e.getValue().asInt()));
            queries.add(new QueryEntry(node.get("queryId").asString(), node.get("query").asString(), relevance));
        }
        return new QrelsFile(root.get("referenceLatitude").asDouble(), root.get("referenceLongitude").asDouble(),
                root.get("radiusKm").asDouble(), root.get("visitAt").asString(), queries);
    }

    private List<UUID> search(String query, double lat, double lng, double radiusKm, String visitAt, String mode)
            throws IOException, InterruptedException {
        String url = API_BASE_URL + "/api/v1/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&latitude=" + lat + "&longitude=" + lng + "&radiusKm=" + radiusKm
                + "&visitAt=" + URLEncoder.encode(visitAt, StandardCharsets.UTF_8)
                + "&size=" + RESULT_SIZE + "&diversify=false&rankingMode=" + mode;
        HttpResponse<String> response = http.send(
                HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("Search API tra ve " + response.statusCode() + " cho query='" + query + "'");
        }
        SearchResponse parsed = mapper.readValue(response.body(), SearchResponse.class);
        return parsed.results().stream().map(SearchResult::poiId).toList();
    }

    private Map<String, double[]> aggregate(List<PerQueryResult> perQuery) {
        Map<String, double[]> sums = new LinkedHashMap<>();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String mode : MODES) {
            sums.put(mode, new double[3]);
            counts.put(mode, 0);
        }
        for (PerQueryResult r : perQuery) {
            double[] s = sums.get(r.mode());
            s[0] += r.pAtK();
            s[1] += r.ap();
            s[2] += r.ndcg();
            counts.merge(r.mode(), 1, Integer::sum);
        }
        sums.forEach((mode, s) -> {
            int n = Math.max(1, counts.get(mode));
            s[0] /= n;
            s[1] /= n;
            s[2] /= n;
        });
        return sums;
    }

    private String renderReport(int queryCount, Map<String, double[]> aggregate, List<PerQueryResult> perQuery) {
        StringBuilder sb = new StringBuilder();
        sb.append("IR EVALUATION (du lieu that: ").append(queryCount).append(" query, API ")
                .append(API_BASE_URL).append(")\n\n");
        sb.append(String.format(Locale.ROOT, "%-14s %8s %8s %10s%n", "Method", "P@" + P_AT, "MAP", "NDCG@" + NDCG_AT));
        sb.append("-".repeat(46)).append('\n');
        Map<String, String> labels = Map.of("keyword", "BM25", "distance", "Distance", "full", "Full Ranking");
        for (String mode : MODES) {
            double[] s = aggregate.get(mode);
            sb.append(String.format(Locale.ROOT, "%-14s %8.4f %8.4f %10.4f%n", labels.get(mode), s[0], s[1], s[2]));
        }
        sb.append("\nPer-query NDCG@").append(NDCG_AT).append(":\n");
        Map<String, List<PerQueryResult>> byQuery = new LinkedHashMap<>();
        for (PerQueryResult r : perQuery) {
            byQuery.computeIfAbsent(r.queryId() + " " + r.query(), k -> new ArrayList<>()).add(r);
        }
        byQuery.forEach((label, rows) -> {
            sb.append("  ").append(label).append('\n');
            rows.forEach(r -> sb.append(String.format(Locale.ROOT, "    %-10s NDCG@%d = %.4f%n",
                    labels.get(r.mode()), NDCG_AT, r.ndcg())));
        });
        return sb.toString();
    }

    private void writeOutputs(Map<String, double[]> aggregate, List<PerQueryResult> perQuery) throws IOException {
        Files.createDirectories(OUTPUT_DIR);

        StringBuilder json = new StringBuilder("{\n  \"aggregate\": {\n");
        List<String> modeLines = new ArrayList<>();
        for (String mode : MODES) {
            double[] s = aggregate.get(mode);
            modeLines.add(String.format(Locale.ROOT, "    \"%s\": {\"pAt%d\": %.4f, \"map\": %.4f, \"ndcgAt%d\": %.4f}",
                    mode, P_AT, s[0], s[1], NDCG_AT, s[2]));
        }
        json.append(String.join(",\n", modeLines)).append("\n  },\n  \"perQuery\": [\n");
        List<String> rowLines = new ArrayList<>();
        for (PerQueryResult r : perQuery) {
            rowLines.add(String.format(Locale.ROOT,
                    "    {\"queryId\": \"%s\", \"query\": \"%s\", \"mode\": \"%s\", \"pAt%d\": %.4f, \"ap\": %.4f, \"ndcgAt%d\": %.4f}",
                    r.queryId(), r.query().replace("\"", "\\\""), r.mode(), P_AT, r.pAtK(), r.ap(), NDCG_AT, r.ndcg()));
        }
        json.append(String.join(",\n", rowLines)).append("\n  ]\n}\n");
        Files.writeString(OUTPUT_DIR.resolve("results.json"), json.toString());

        StringBuilder csv = new StringBuilder("queryId,query,mode,pAt" + P_AT + ",ap,ndcgAt" + NDCG_AT + "\n");
        for (PerQueryResult r : perQuery) {
            csv.append(r.queryId()).append(',').append('"').append(r.query()).append('"').append(',')
                    .append(r.mode()).append(',').append(r.pAtK()).append(',').append(r.ap()).append(',')
                    .append(r.ndcg()).append('\n');
        }
        Files.writeString(OUTPUT_DIR.resolve("results.csv"), csv.toString());
    }
}
