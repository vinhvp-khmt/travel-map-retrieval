package com.travelmap.api.search.eval;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Các độ đo xếp hạng cho bộ đánh giá 2.4 — thuần toán, không phụ thuộc Spring/DB.
 *
 * <p>Ba độ đo:
 * <ul>
 *   <li>{@link #precisionAtK} — P@k: tỉ lệ tài liệu liên quan trong k vị trí đầu (chia cho k).</li>
 *   <li>{@link #averagePrecision} — AP: trung bình precision tại mỗi vị trí trúng, chia cho tổng
 *       số tài liệu liên quan (nền tảng của MAP khi lấy trung bình trên nhiều truy vấn).</li>
 *   <li>{@link #ndcgAtK} — NDCG@k dùng gain {@code 2^grade - 1} và chiết khấu {@code log2(i+2)}.</li>
 * </ul>
 *
 * <p>Quy ước: {@code rel} là tập tài liệu "liên quan" (nhị phân, thường là grade ≥ 1) dùng cho
 * P@k và AP; {@code grades} là bảng nhãn phân cấp (0..n) dùng cho NDCG.
 */
public final class Metrics {

    private Metrics() {
    }

    /**
     * Precision@k: trong {@code k} vị trí đầu của danh sách xếp hạng có bao nhiêu tài liệu liên quan,
     * chia cho {@code k}. Nếu danh sách ngắn hơn {@code k} thì mẫu số vẫn là {@code k} (đúng định
     * nghĩa P@k chuẩn). {@code k <= 0} trả về 0.
     */
    public static double precisionAtK(List<UUID> ranked, Set<UUID> rel, int k) {
        if (k <= 0) {
            return 0.0;
        }
        int n = Math.min(k, ranked.size());
        long hit = ranked.subList(0, n).stream().filter(rel::contains).count();
        return (double) hit / k;
    }

    /**
     * Average Precision cho một truy vấn: cộng precision tại từng vị trí trúng tài liệu liên quan,
     * rồi chia cho tổng số tài liệu liên quan. Truy vấn không có tài liệu liên quan trả về 0.
     * Lấy trung bình AP trên toàn bộ truy vấn cho ra MAP.
     */
    public static double averagePrecision(List<UUID> ranked, Set<UUID> rel) {
        if (rel.isEmpty()) {
            return 0.0;
        }
        double sum = 0.0;
        int hit = 0;
        for (int i = 0; i < ranked.size(); i++) {
            if (rel.contains(ranked.get(i))) {
                hit++;
                sum += (double) hit / (i + 1);
            }
        }
        return sum / rel.size();
    }

    /**
     * NDCG@k với gain {@code 2^grade - 1} và chiết khấu logarit cơ số 2.
     * DCG lấy trên {@code k} vị trí đầu của danh sách; IDCG lấy trên {@code k} nhãn cao nhất trong
     * {@code grades}. Tài liệu không có trong {@code grades} coi như grade 0. IDCG = 0 trả về 0.
     */
    public static double ndcgAtK(List<UUID> ranked, Map<UUID, Integer> grades, int k) {
        if (k <= 0) {
            return 0.0;
        }
        double dcg = 0.0;
        int limit = Math.min(k, ranked.size());
        for (int i = 0; i < limit; i++) {
            int g = grades.getOrDefault(ranked.get(i), 0);
            dcg += (Math.pow(2, g) - 1) / (Math.log(i + 2) / Math.log(2));
        }
        List<Integer> ideal = grades.values().stream()
                .sorted(Comparator.reverseOrder())
                .limit(k)
                .toList();
        double idcg = 0.0;
        for (int i = 0; i < ideal.size(); i++) {
            idcg += (Math.pow(2, ideal.get(i)) - 1) / (Math.log(i + 2) / Math.log(2));
        }
        return idcg == 0.0 ? 0.0 : dcg / idcg;
    }
}
