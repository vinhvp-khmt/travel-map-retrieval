package com.travelmap.api.search;

import com.travelmap.api.search.model.WeightProfile;
import com.travelmap.api.search.service.RankingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void v2FrameworkMatchesV1WhileFormulasUnchanged() {
        // Giai đoạn 2.1: spatialDecay/ratingShrinkage tạm bằng công thức cũ,
        // nên V2 phải cho cùng điểm với V1.
        var v1 = ranking.score(1, 2, 500, 2_000, 1, 4);
        var v2 = ranking.score(WeightProfile.V2, 1, 2, 500, 2_000, 1, 4, 10, 3.7);
        assertEquals(v1.finalScore(), v2.finalScore(), 1e-9);
        assertEquals(v1.spatial(), v2.spatial(), 1e-9);
        assertEquals(v1.rating(), v2.rating(), 1e-9);
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
    void keywordOnlyProfileIgnoresDistanceAndRating() {
        // EVAL_KEYWORD_ONLY chỉ tính BM25 → điểm cuối = bm25 chuẩn hoá,
        // bất kể khoảng cách hay đánh giá.
        var score = ranking.score(WeightProfile.EVAL_KEYWORD_ONLY,
                1, 2, 1_999, 2_000, 0, 1, 0, 3.7);
        assertEquals(0.5, score.finalScore(), 1e-9); // 1/2 = 0.5
    }
}
