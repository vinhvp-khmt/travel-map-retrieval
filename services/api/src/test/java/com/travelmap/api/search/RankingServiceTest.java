package com.travelmap.api.search;

import com.travelmap.api.search.model.WeightProfile;
import com.travelmap.api.search.service.RankingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankingServiceTest {
    private final RankingService ranking = new RankingService();

    // ===== Test cũ — phải vẫn xanh (chứng minh V1 không đổi hành vi) =====

    @Test
    void appliesDocumentedWeightedFormula() {
        var score = ranking.score(2, 2, 0, 2_000, 1, 5);
        assertEquals(1.0, score.finalScore());
    }

    @Test
    void openCandidateScoresAboveClosedCandidateWhenOtherSignalsTie() {
        var open = ranking.score(1, 1, 500, 2_000, 1, 4);
        var closed = ranking.score(1, 1, 500, 2_000, 0, 4);
        assertTrue(open.finalScore() > closed.finalScore());
    }

    // ===== Test mới cho phần 2.1 (khung v1/v2) =====

    @Test
    void v2DiffersFromV1AfterFormulasImplemented() {
        // Phần 2.2: spatialDecay + ratingShrinkage đã cắm công thức thật,
        // nên V2 phải KHÁC V1 ở cả điểm không gian lẫn điểm đánh giá.
        // (Thay cho test cũ v2FrameworkMatchesV1WhileFormulasUnchanged của 2.1.)
        var v1 = ranking.score(1, 2, 500, 2_000, 1, 4);
        var v2 = ranking.score(WeightProfile.V2, 1, 2, 500.0, 2_000, 1, 4, 10, 3.7);
        assertNotEquals(v1.spatial(), v2.spatial(),
                "V2 phải dùng spatial decay, khác linear của V1");
        assertNotEquals(v1.rating(), v2.rating(),
                "V2 phải dùng rating shrinkage, khác rating thô của V1");
    }

    // ===== Test mới cho phần 2.2 (spatial decay + rating shrinkage) =====

    @Test
    void rating_nhieu_luot_thang_it_luot() {
        // 5★ nhưng chỉ 1 lượt phải thua 4.5★ với 200 lượt, nhờ Bayesian shrinkage.
        double gm = 3.7;
        var it = ranking.score(WeightProfile.V2, 1, 1, 0.0, 2_000, 1, 5.0, 1, gm);
        var nhieu = ranking.score(WeightProfile.V2, 1, 1, 0.0, 2_000, 1, 4.5, 200, gm);
        assertTrue(nhieu.rating() > it.rating(),
                "quán 4.5★/200 lượt phải có điểm rating cao hơn quán 5★/1 lượt");
    }

    @Test
    void spatial_decay_giam_dan() {
        // Quán gần phải có điểm không gian cao hơn quán xa, và điểm vẫn dương ở rìa.
        var gan = ranking.score(WeightProfile.V2, 1, 1, 100.0, 2_000, 1, 4, 10, 3.7);
        var xa = ranking.score(WeightProfile.V2, 1, 1, 1_500.0, 2_000, 1, 4, 10, 3.7);
        assertTrue(gan.spatial() > xa.spatial(), "gần phải hơn xa");
        assertTrue(xa.spatial() > 0, "suy giảm mượt, không tụt về 0 đột ngột");
    }

    @Test
    void v1_khong_bi_anh_huong() {
        // V1 phải giữ nguyên công thức tuyến tính + rating thô như trước.
        var v1 = ranking.score(1, 1, 1_000, 2_000, 1, 5);
        assertEquals(0.5, v1.spatial(), 1e-6);   // linear: 1 - 1000/2000
        assertEquals(1.0, v1.rating(), 1e-6);    // rating thô: 5/5
    }

    @Test
    void profileFromDefaultsToV1OnMissingOrUnknown() {
        assertEquals(WeightProfile.V1, WeightProfile.from(null));
        assertEquals(WeightProfile.V1, WeightProfile.from("linh tinh"));
        assertEquals(WeightProfile.V1, WeightProfile.from("V1"));
        assertEquals(WeightProfile.V2, WeightProfile.from("v2"));
        assertEquals(WeightProfile.V2, WeightProfile.from("  V2 "));
    }

    @Test
    void profileFromAcceptsRankingModeVocabulary() {
        // Phase 2.9 (dataset/evaluation plan): rankingMode=keyword|distance|full phải
        // map đúng 3 baseline dùng để so sánh trong IrEvaluationTest.
        assertEquals(WeightProfile.EVAL_KEYWORD_ONLY, WeightProfile.from("keyword"));
        assertEquals(WeightProfile.EVAL_DISTANCE_ONLY, WeightProfile.from("distance"));
        assertEquals(WeightProfile.V2, WeightProfile.from("full"));
        assertEquals(WeightProfile.EVAL_KEYWORD_ONLY, WeightProfile.from("  KEYWORD "));
    }

    @Test
    void keywordOnlyProfileIgnoresDistanceAndRating() {
        // EVAL_KEYWORD_ONLY chỉ tính BM25 → điểm cuối = bm25 chuẩn hoá,
        // bất kể khoảng cách hay đánh giá.
        var score = ranking.score(WeightProfile.EVAL_KEYWORD_ONLY,
                1, 2, 1_999.0, 2_000, 0, 1, 0, 3.7);
        assertEquals(0.5, score.finalScore(), 1e-9); // 1/2 = 0.5
    }

    @Test
    void khoangCach_cungViTri_0_5km_5km_ratXa_phaiGiamDan() {
        // Phần 2.6/2.10 test bắt buộc: cùng vị trí (0m) > 0.5km > 5km > rất xa (50km),
        // với bán kính tìm kiếm cố định 5km — kiểm cả hai cách tính (V1 tuyến tính, V2 decay).
        double radiusMeters = 5_000;
        double sameLocation = ranking.score(WeightProfile.V1, 1, 1, 0.0, radiusMeters, 1, 0, 0, 0).spatial();
        double halfKm = ranking.score(WeightProfile.V1, 1, 1, 500.0, radiusMeters, 1, 0, 0, 0).spatial();
        double fiveKm = ranking.score(WeightProfile.V1, 1, 1, 5_000.0, radiusMeters, 1, 0, 0, 0).spatial();
        double veryFar = ranking.score(WeightProfile.V1, 1, 1, 50_000.0, radiusMeters, 1, 0, 0, 0).spatial();
        assertEquals(1.0, sameLocation, 1e-9, "cùng vị trí (0m) phải đạt điểm tối đa");
        assertTrue(sameLocation > halfKm, "cùng vị trí phải hơn 0.5km");
        assertTrue(halfKm > fiveKm, "0.5km phải hơn 5km");
        assertTrue(fiveKm >= veryFar, "5km phải hơn hoặc bằng rất xa (cả hai đều ở rìa/ngoài bán kính)");
        assertEquals(0.0, veryFar, 1e-9, "rất xa (ngoài bán kính rất nhiều) phải về 0, không âm");

        double sameLocationV2 = ranking.score(WeightProfile.V2, 1, 1, 0.0, radiusMeters, 1, 0, 0, 0).spatial();
        double halfKmV2 = ranking.score(WeightProfile.V2, 1, 1, 500.0, radiusMeters, 1, 0, 0, 0).spatial();
        double fiveKmV2 = ranking.score(WeightProfile.V2, 1, 1, 5_000.0, radiusMeters, 1, 0, 0, 0).spatial();
        double veryFarV2 = ranking.score(WeightProfile.V2, 1, 1, 50_000.0, radiusMeters, 1, 0, 0, 0).spatial();
        assertTrue(sameLocationV2 > halfKmV2 && halfKmV2 > fiveKmV2 && fiveKmV2 > veryFarV2,
                "V2 (decay) cũng phải giảm dần đúng thứ tự cùng vị trí > 0.5km > 5km > rất xa");
    }

    @Test
    void thieuGpsDungSpatialScoreTrungLap() {
        // Phần 2.4 (dataset/evaluation plan): thiếu GPS không được làm search fail, và
        // không được ưu ái/phạt oan bất kỳ quán nào — spatial phải luôn ra đúng 0.5,
        // bất kể profile (LINEAR hay DECAY) hay bán kính.
        var v1 = ranking.score(WeightProfile.V1, 1, 1, null, 2_000, 1, 4, 0, 0);
        var v2 = ranking.score(WeightProfile.V2, 1, 1, null, 2_000, 1, 4, 10, 3.7);
        assertEquals(0.5, v1.spatial(), 1e-9);
        assertEquals(0.5, v2.spatial(), 1e-9);
    }

    @Test
    void scoreDetailExplainsAndReproducesTheFinalScore() {
        var score = ranking.score(WeightProfile.V2,
                1.5, 2.0, 420, 2_000, 0.5, 4.5, 200, 3.8);

        assertEquals("full-v2", score.rankingProfile());
        assertEquals(1.5, score.rawBm25());
        assertEquals(420, score.distanceMeters());
        assertEquals(4.5, score.averageRating());
        assertEquals(200, score.ratingCount());
        assertEquals(score.finalScore(), score.bm25Contribution() + score.spatialContribution()
                + score.temporalContribution() + score.ratingContribution(), 0.0002);
    }

    @Test
    void publicProfileNamesResolveToAllComparisonStrategies() {
        assertEquals(WeightProfile.V1, WeightProfile.from("full-v1"));
        assertEquals(WeightProfile.V2, WeightProfile.from("full-v2"));
        assertEquals(WeightProfile.EVAL_KEYWORD_ONLY, WeightProfile.from("bm25-only"));
        assertEquals(WeightProfile.EVAL_DISTANCE_ONLY, WeightProfile.from("distance-only"));
    }
}
