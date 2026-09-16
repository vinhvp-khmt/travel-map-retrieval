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
 * Hai profile phục vụ đánh giá ở phần 2.4 ({@link #EVAL_KEYWORD_ONLY},
 * {@link #EVAL_DISTANCE_ONLY}) cố tình phá bất biến đó — chúng chỉ dùng trong test.
 */
public enum WeightProfile {

    /** Bản gốc — không đổi hành vi so với công thức đã có. */
    V1(0.40, 0.30, 0.20, 0.10, SpatialMode.LINEAR, RatingMode.RAW),

    /** Bản thử — spatial decay + rating shrinkage (ruột công thức làm ở phần 2.2). */
    V2(0.40, 0.30, 0.20, 0.10, SpatialMode.DECAY, RatingMode.SHRINKAGE),

    /** Chỉ dùng cho đánh giá (2.4): chỉ tính BM25 (khớp từ khoá). */
    EVAL_KEYWORD_ONLY(1.00, 0.00, 0.00, 0.00, SpatialMode.LINEAR, RatingMode.RAW),

    /** Chỉ dùng cho đánh giá (2.4): chỉ tính khoảng cách. */
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
     * <p>Chỉ chấp nhận "v1"/"v2" từ phía client; hai profile EVAL_* dành riêng cho
     * bộ đánh giá nội bộ (2.4), không lộ qua API.
     */
    public static WeightProfile from(String raw) {
        if (raw == null) {
            return V1;
        }
        return switch (raw.trim().toLowerCase()) {
            case "v2" -> V2;
            default -> V1;
        };
    }
}
