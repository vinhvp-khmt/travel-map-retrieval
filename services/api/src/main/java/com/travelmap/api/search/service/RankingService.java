package com.travelmap.api.search.service;

import com.travelmap.api.search.dto.ScoreDetail;
import com.travelmap.api.search.model.WeightProfile;
import org.springframework.stereotype.Service;

/**
 * Tính điểm xếp hạng cuối cùng từ bốn tín hiệu: BM25, không gian, thời gian, đánh giá.
 *
 * <p>Phần 2.1 (nâng cấp an toàn): hỗ trợ chọn {@link WeightProfile}.
 * <ul>
 *   <li>Hàm {@link #score(double, double, double, double, double, double)} cũ được
 *       GIỮ NGUYÊN chữ ký và hành vi — nó chính là {@code V1}.</li>
 *   <li>Hàm {@link #score(WeightProfile, double, double, double, double, double, double, long, double)}
 *       mới rẽ nhánh công thức theo profile.</li>
 * </ul>
 *
 * <p>Ở phần 2.1, hai công thức mới ({@link #spatialDecay}, {@link #ratingShrinkage})
 * tạm trả về đúng kết quả của công thức cũ, nên {@code V2} chạy giống hệt {@code V1}.
 * Phần 2.2 sẽ thay ruột hai hàm này bằng spatial decay + Bayesian shrinkage thật.
 */
@Service
public class RankingService {

    /**
     * Công thức gốc (V1) — không đổi một dòng logic nào so với bản trước.
     * Giữ lại để mọi lời gọi cũ và test cũ tiếp tục hoạt động.
     */
    public ScoreDetail score(double rawBm25, double maxBm25, double distanceMeters,
                             double radiusMeters, double temporalFit, double averageRating) {
        return score(WeightProfile.V1, rawBm25, maxBm25, distanceMeters, radiusMeters,
                temporalFit, averageRating, 0, 0);
    }

    /**
     * Công thức có chọn profile.
     *
     * @param profile          bộ trọng số + chiến lược spatial/rating
     * @param rawBm25          điểm BM25 thô của tài liệu
     * @param maxBm25          BM25 lớn nhất trong tập candidate (để chuẩn hoá)
     * @param distanceMeters   khoảng cách tới người dùng (m)
     * @param radiusMeters     bán kính tìm kiếm (m)
     * @param temporalFit      độ khớp giờ mở cửa trong [0,1]
     * @param averageRating    điểm sao trung bình của quán [0,5]
     * @param ratingCount      số lượt đánh giá (chỉ V2 dùng)
     * @param globalMeanRating điểm sao trung bình toàn hệ thống, làm prior (chỉ V2 dùng)
     */
    public ScoreDetail score(WeightProfile profile, double rawBm25, double maxBm25,
                             double distanceMeters, double radiusMeters,
                             double temporalFit, double averageRating,
                             long ratingCount, double globalMeanRating) {
        double bm25 = maxBm25 <= 0 ? 0 : clamp(rawBm25 / maxBm25);

        double spatial = switch (profile.spatialMode()) {
            case LINEAR -> clamp(1.0 - distanceMeters / radiusMeters);
            case DECAY -> spatialDecay(distanceMeters, radiusMeters);
        };

        double temporal = clamp(temporalFit);

        double rating = switch (profile.ratingMode()) {
            case RAW -> clamp(averageRating / 5.0);
            case SHRINKAGE -> ratingShrinkage(averageRating, ratingCount, globalMeanRating);
        };

        double finalScore = profile.weightBm25() * bm25
                + profile.weightSpatial() * spatial
                + profile.weightTemporal() * temporal
                + profile.weightRating() * rating;

        return new ScoreDetail(round(bm25), round(spatial), round(temporal),
                round(rating), round(finalScore));
    }

    /**
     * Điểm không gian theo kiểu suy giảm mượt.
     *
     * <p>TODO (phần 2.2): thay bằng {@code exp(-distance / (radius/3))} hoặc half-life.
     * Hiện tạm dùng công thức tuyến tính để {@code V2} chạy giống {@code V1}.
     */
    static double spatialDecay(double distanceMeters, double radiusMeters) {
        return clamp(1.0 - distanceMeters / radiusMeters);
    }

    /**
     * Điểm đánh giá có hiệu chỉnh theo số lượt.
     *
     * <p>TODO (phần 2.2): thay bằng Bayesian shrinkage
     * {@code adjusted = v/(v+m)*R + m/(v+m)*C}. Hiện tạm dùng {@code averageRating/5}
     * để {@code V2} chạy giống {@code V1}.
     */
    static double ratingShrinkage(double averageRating, long ratingCount, double globalMeanRating) {
        return clamp(averageRating / 5.0);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
