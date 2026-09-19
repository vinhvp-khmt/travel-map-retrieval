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
 * <p>Phần 2.2 (đã cắm công thức thật): {@link #spatialDecay} dùng suy giảm theo hàm mũ
 * và {@link #ratingShrinkage} dùng Bayesian shrinkage, nên từ nay {@code V2} cho kết quả
 * KHÁC {@code V1} — quán nhiều lượt đánh giá được ưu tiên hơn và điểm khoảng cách mượt hơn.
 * {@code V1} vẫn giữ nguyên tuyệt đối hành vi cũ.
 */
@Service
public class RankingService {

    /**
     * Điểm không gian trung lập dùng khi request không có toạ độ user (phần 2.4: GPS là
     * tuỳ chọn). 0.5 = "không có thông tin", không thiên vị quán gần hay xa — khác 0 (sẽ
     * phạt oan mọi quán) và khác 1 (sẽ ưu ái oan mọi quán).
     */
    static final double NEUTRAL_SPATIAL_SCORE = 0.5;

    /**
     * Số lượt đánh giá "ảo" của prior trong Bayesian shrinkage (tham số {@code m}).
     * Càng lớn thì càng cần nhiều lượt thật mới kéo điểm ra khỏi trung bình chung.
     */
    private static final double DEFAULT_CONFIDENCE_M = 20.0;

    /**
     * Prior dự phòng khi hệ thống chưa có {@code globalMeanRating} hợp lệ
     * (ví dụ tập kết quả rỗng). Chọn 3.7 ≈ mức trung bình quán ăn/đồ uống điển hình.
     */
    private static final double DEFAULT_FALLBACK_PRIOR = 3.7;

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
     * @param distanceMeters   khoảng cách tới người dùng (m); {@code null} nếu request không
     *                         có GPS — khi đó spatial dùng {@link #NEUTRAL_SPATIAL_SCORE}
     *                         thay vì tính theo khoảng cách thật (phần 2.4)
     * @param radiusMeters     bán kính tìm kiếm (m)
     * @param temporalFit      độ khớp giờ mở cửa trong [0,1]
     * @param averageRating    điểm sao trung bình của quán [0,5]
     * @param ratingCount      số lượt đánh giá (chỉ V2 dùng)
     * @param globalMeanRating điểm sao trung bình toàn hệ thống, làm prior (chỉ V2 dùng)
     */
    public ScoreDetail score(WeightProfile profile, double rawBm25, double maxBm25,
                             Double distanceMeters, double radiusMeters,
                             double temporalFit, double averageRating,
                             long ratingCount, double globalMeanRating) {
        double bm25 = maxBm25 <= 0 ? 0 : clamp(rawBm25 / maxBm25);

        double spatial = distanceMeters == null ? NEUTRAL_SPATIAL_SCORE : switch (profile.spatialMode()) {
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

        double bm25Contribution = profile.weightBm25() * bm25;
        double spatialContribution = profile.weightSpatial() * spatial;
        double temporalContribution = profile.weightTemporal() * temporal;
        double ratingContribution = profile.weightRating() * rating;

        return new ScoreDetail(round(bm25), round(spatial), round(temporal),
                round(rating), round(finalScore), profile.apiName(), round(rawBm25),
                distanceMeters == null ? 0.0 : round(distanceMeters), round(averageRating), ratingCount,
                profile.weightBm25(), profile.weightSpatial(), profile.weightTemporal(), profile.weightRating(),
                round(bm25Contribution), round(spatialContribution), round(temporalContribution),
                round(ratingContribution));
    }

    /**
     * Điểm không gian theo kiểu suy giảm mượt (exponential decay).
     *
     * <p>Phần 2.2: thay công thức tuyến tính bằng suy giảm theo hàm mũ
     * {@code exp(-distance / scale)} với {@code scale = radius / 3}. So với tuyến tính,
     * cách này phạt nhẹ ở gần và phạt nặng dần khi ra xa, nhưng không bao giờ tụt về 0
     * đột ngột tại đúng biên bán kính — nhờ vậy thứ tự các quán mượt và ổn định hơn.
     *
     * <p>Vì sao chia 3: tại {@code distance = radius} điểm còn {@code exp(-3) ≈ 0.05}
     * (quán ở rìa vẫn được tính chút ít); tại {@code distance = radius/3} điểm còn
     * {@code exp(-1) ≈ 0.37}. Kết quả luôn nằm trong [0,1] nhờ {@link #clamp}.
     *
     * @param distanceMeters khoảng cách tới người dùng (m), không âm
     * @param radiusMeters   bán kính tìm kiếm (m); {@code <= 0} coi như không có tín hiệu → 0
     */
    static double spatialDecay(double distanceMeters, double radiusMeters) {
        if (radiusMeters <= 0) {
            return 0;
        }
        double scale = radiusMeters / 3.0;
        return clamp(Math.exp(-distanceMeters / scale));
    }

    /**
     * Điểm đánh giá có hiệu chỉnh theo số lượt (Bayesian shrinkage).
     *
     * <p>Phần 2.2: giải bài toán "5★ với 1 lượt" trông ngon hơn "4.5★ với 200 lượt".
     * Công thức kéo điểm ít lượt về gần điểm trung bình toàn hệ thống (prior {@code C}):
     * <pre>{@code adjusted = v/(v+m) * R + m/(v+m) * C}</pre>
     * với {@code R = averageRating}, {@code v = ratingCount}, {@code C = globalMeanRating}
     * và {@code m = }{@value #DEFAULT_CONFIDENCE_M} (số lượt "ảo" của prior). Quán càng
     * nhiều lượt ({@code v} lớn) thì càng tin vào điểm thật của nó; quán ít lượt bị kéo
     * về prior nên không dễ vọt lên đầu chỉ nhờ vài lượt 5★.
     *
     * <p>Ví dụ (C = 3.7): 5★/1 lượt → adjusted ≈ 3.76; 4.5★/200 lượt → adjusted ≈ 4.43
     * ⇒ quán nhiều lượt thắng. Điểm trả về là {@code adjusted / 5} nằm trong [0,1].
     *
     * @param averageRating    điểm sao trung bình của quán [0,5]
     * @param ratingCount      số lượt đánh giá (âm được coi như 0)
     * @param globalMeanRating prior — điểm sao trung bình toàn hệ thống;
     *                         {@code <= 0} sẽ dùng mặc định {@value #DEFAULT_FALLBACK_PRIOR}
     */
    static double ratingShrinkage(double averageRating, long ratingCount, double globalMeanRating) {
        double v = Math.max(0, ratingCount);
        double m = DEFAULT_CONFIDENCE_M;
        double c = globalMeanRating <= 0 ? DEFAULT_FALLBACK_PRIOR : globalMeanRating;
        double adjusted = (v / (v + m)) * averageRating + (m / (v + m)) * c;
        return clamp(adjusted / 5.0);
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }
}
