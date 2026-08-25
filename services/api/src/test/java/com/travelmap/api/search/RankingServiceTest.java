package com.travelmap.api.search;

import com.travelmap.api.search.service.RankingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankingServiceTest {
    private final RankingService ranking = new RankingService();

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
}
