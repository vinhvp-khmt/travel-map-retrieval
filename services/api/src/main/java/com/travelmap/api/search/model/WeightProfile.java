package com.travelmap.api.search.model;

/**
 * Bộ trọng số / cấu hình công thức xếp hạng.
 *
 * <p>Mục đích (phần 2.1 - nâng cấp an toàn): chạy song song hai công thức xếp hạng.
 * <ul>
 *   <li>{@link #V1} — bản gốc, GIỮ NGUYÊN TUYỆT ĐỐI (linear spatial + rating thô).</li>
 *   <li>{@link #V2} — bản thử: dùng spatial decay + rating shrinkage (cắm ở phần 2.2).</li>
 * </ul>
 *
 * <p>Enum này gói cả 4 trọng số lẫn hai "cờ chiến lược"
 * ({@link SpatialMode}, {@link RatingMode}). Nhờ vậy {@code RankingService} dùng
 * chung MỘT hàm {@code score(...)} nhưng rẽ nhánh công thức bên trong theo profile,
 * thay vì phải nhân bản code.
 *
 * <p><b>Bất biến:</b> tổng bốn trọng số nên bằng 1.0 để điểm cuối nằm trong [0,1].
 * Hai profile baseline ({@link #EVAL_KEYWORD_ONLY}, {@link #EVAL_DISTANCE_ONLY}) cố tình
 * phá bất biến đó — chỉ dùng một tín hiệu duy nhất để so sánh với ranking đầy đủ.
 *
 * <p>Từ IR search plan (dataset/evaluation): ba baseline này lộ qua API bằng tham số
 * {@code rankingMode=keyword|distance|full} (xem {@link #from(String)}), để chạy cùng một
 * bộ query trên cả ba cấu hình và so sánh P@5/MAP/NDCG@10 — không cần UI riêng cho từng mode.
 */
public enum WeightProfile {

    /** Bản gốc — không đổi hành vi so với công thức đã có. */
    V1(0.40, 0.30, 0.20, 0.10, SpatialMode.LINEAR, RatingMode.RAW),

    /** Bản thử — spatial decay + rating shrinkage (ruột công thức làm ở phần 2.2). */
    V2(0.40, 0.30, 0.20, 0.10, SpatialMode.DECAY, RatingMode.SHRINKAGE),

    /** Baseline 1 (rankingMode=keyword): chỉ tính BM25 (khớp từ khoá). */
    EVAL_KEYWORD_ONLY(1.00, 0.00, 0.00, 0.00, SpatialMode.LINEAR, RatingMode.RAW),

    /** Baseline 2 (rankingMode=distance): chỉ tính khoảng cách. */
    EVAL_DISTANCE_ONLY(0.00, 1.00, 0.00, 0.00, SpatialMode.LINEAR, RatingMode.RAW);

    /** Cách tính điểm không gian. */
    public enum SpatialMode {
        /** {@code spatial = 1 - distance/radius} (tuyến tính, như bản gốc). */
        LINEAR,
        /** Suy giảm mượt theo khoảng cách (exp / half-life) — xem phần 2.2. */
        DECAY
    }

    /** Cách tính điểm đánh giá sao. */
    public enum RatingMode {
        /** {@code rating = avgRating / 5} (thô, như bản gốc). */
        RAW,
        /** Hiệu chỉnh theo số lượt (Bayesian shrinkage) — xem phần 2.2. */
        SHRINKAGE
    }

    private final double wBm25;
    private final double wSpatial;
    private final double wTemporal;
    private final double wRating;
    private final SpatialMode spatialMode;
    private final RatingMode ratingMode;

    WeightProfile(double wBm25, double wSpatial, double wTemporal, double wRating,
                  SpatialMode spatialMode, RatingMode ratingMode) {
        this.wBm25 = wBm25;
        this.wSpatial = wSpatial;
        this.wTemporal = wTemporal;
        this.wRating = wRating;
        this.spatialMode = spatialMode;
        this.ratingMode = ratingMode;
    }

    public double weightBm25() {
        return wBm25;
    }

    public double weightSpatial() {
        return wSpatial;
    }

    public double weightTemporal() {
        return wTemporal;
    }

    public double weightRating() {
        return wRating;
    }

    public SpatialMode spatialMode() {
        return spatialMode;
    }

    public RatingMode ratingMode() {
        return ratingMode;
    }

    /**
     * Phân giải profile từ tham số truy vấn (không phân biệt hoa/thường, tự trim).
     * An toàn: giá trị null hoặc không hợp lệ đều rơi về {@link #V1} thay vì báo lỗi,
     * để một tham số sai không bao giờ làm hỏng truy vấn tìm kiếm.
     *
     * <p>Chấp nhận hai bộ từ khoá tương đương:
     * <ul>
     *   <li>{@code v1}/{@code v2} — chọn công thức ranking đầy đủ (bản gốc / bản thử).</li>
     *   <li>{@code keyword}/{@code distance}/{@code full} — ba baseline dùng cho evaluation
     *       (Phase 2.9): chỉ BM25, chỉ khoảng cách, hoặc ranking đầy đủ (= {@code v2},
     *       công thức đã được {@code IrEvaluationTest} xác nhận thắng cả hai baseline).</li>
     * </ul>
     */
    public static WeightProfile from(String raw) {
        if (raw == null) {
            return V1;
        }
        return switch (raw.trim().toLowerCase()) {
            case "v2", "full" -> V2;
            case "keyword" -> EVAL_KEYWORD_ONLY;
            case "distance" -> EVAL_DISTANCE_ONLY;
            default -> V1;
        };
    }
}
