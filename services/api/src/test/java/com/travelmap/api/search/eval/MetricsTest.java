package com.travelmap.api.search.eval;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Kiểm thử "tính tay" cho ba độ đo: mỗi ví dụ nhỏ, kết quả kỳ vọng được tính bằng tay trong
 * chú thích rồi assert lại. Mục đích: khẳng định công thức P@k / MAP / NDCG@k chạy đúng định
 * nghĩa chuẩn trước khi dùng chúng cho bộ đánh giá lớn (IrEvaluationTest).
 */
class MetricsTest {

    // Sáu tài liệu giả lập với UUID cố định cho dễ theo dõi.
    private static final UUID U1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID U2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID U3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID U4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID U5 = UUID.fromString("00000000-0000-0000-0000-000000000005");

    @Test
    @DisplayName("P@k: đếm số tài liệu liên quan trong k đầu, chia cho k")
    void precisionAtK_tinh_tay() {
        // Xếp hạng: U1 U2 U3 U4 U5 ; liên quan = {U1, U3, U5}.
        List<UUID> ranked = List.of(U1, U2, U3, U4, U5);
        Set<UUID> rel = Set.of(U1, U3, U5);

        // P@1: top1 = [U1] -> trúng 1 -> 1/1 = 1.0
        assertEquals(1.0, Metrics.precisionAtK(ranked, rel, 1), 1e-9);
        // P@3: top3 = [U1,U2,U3] -> trúng U1,U3 = 2 -> 2/3
        assertEquals(2.0 / 3.0, Metrics.precisionAtK(ranked, rel, 3), 1e-9);
        // P@5: top5 -> trúng U1,U3,U5 = 3 -> 3/5 = 0.6
        assertEquals(0.6, Metrics.precisionAtK(ranked, rel, 5), 1e-9);
        // k = 0 -> quy ước trả về 0 (không chia cho 0)
        assertEquals(0.0, Metrics.precisionAtK(ranked, rel, 0), 1e-9);
    }

    @Test
    @DisplayName("AP: trung bình precision tại các vị trí trúng, chia cho số tài liệu liên quan")
    void averagePrecision_tinh_tay() {
        // Xếp hạng: U1 U2 U3 U4 U5 ; liên quan = {U1, U3, U5} (3 tài liệu).
        // Trúng tại: hạng 1 (U1) -> 1/1 ; hạng 3 (U3) -> 2/3 ; hạng 5 (U5) -> 3/5.
        // AP = (1 + 2/3 + 3/5) / 3 = 2.26666.../3 = 0.755555...
        List<UUID> ranked = List.of(U1, U2, U3, U4, U5);
        Set<UUID> rel = Set.of(U1, U3, U5);
        double expected = (1.0 + 2.0 / 3.0 + 3.0 / 5.0) / 3.0;
        assertEquals(expected, Metrics.averagePrecision(ranked, rel), 1e-9);
        assertEquals(0.7555555555, Metrics.averagePrecision(ranked, rel), 1e-6);

        // Không có tài liệu liên quan -> AP = 0.
        assertEquals(0.0, Metrics.averagePrecision(ranked, Set.of()), 1e-9);
    }

    @Test
    @DisplayName("NDCG@k: gain 2^grade-1, chiết khấu log2(i+2), chuẩn hoá theo IDCG")
    void ndcgAtK_tinh_tay() {
        // Xếp hạng: U1 U2 U3 ; nhãn: U1=3, U2=0, U3=1 ; ngoài ra U4=2 (không được truy hồi).
        // DCG = (2^3-1)/log2(2) + (2^0-1)/log2(3) + (2^1-1)/log2(4)
        //     = 7/1 + 0 + 1/2 = 7.5
        // IDCG (nhãn tốt nhất 3,2,1) = 7/1 + 3/log2(3) + 1/log2(4)
        //     = 7 + 3/1.5849625 + 0.5 = 9.392789...
        // NDCG = 7.5 / 9.392789... = 0.7984848...
        List<UUID> ranked = List.of(U1, U2, U3);
        Map<UUID, Integer> grades = Map.of(U1, 3, U3, 1, U4, 2);

        double dcg = 7.0 + 0.0 + 1.0 / 2.0;
        double idcg = 7.0 + 3.0 / (Math.log(3) / Math.log(2)) + 1.0 / 2.0;
        double expected = dcg / idcg;
        assertEquals(expected, Metrics.ndcgAtK(ranked, grades, 3), 1e-9);
        assertEquals(0.7984848581, Metrics.ndcgAtK(ranked, grades, 3), 1e-6);

        // Xếp hạng lý tưởng phải cho NDCG = 1.0 (nhãn giảm dần khớp thứ tự tốt nhất).
        List<UUID> ideal = List.of(U1, U4, U3); // grades 3, 2, 1
        assertEquals(1.0, Metrics.ndcgAtK(ideal, grades, 3), 1e-9);

        // Không có nhãn nào -> NDCG = 0.
        assertTrue(Metrics.ndcgAtK(ranked, Map.of(), 3) == 0.0, "IDCG=0 phải trả về 0");
    }
}
