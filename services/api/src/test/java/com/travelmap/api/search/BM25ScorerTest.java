package com.travelmap.api.search;

import com.travelmap.api.search.service.BM25Scorer;
import com.travelmap.api.search.service.InvertedIndex;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BM25ScorerTest {
    @Test
    void repeatedRelevantTermScoresAboveNonMatchingDocument() {
        UUID relevant = UUID.randomUUID();
        UUID unrelated = UUID.randomUUID();
        InvertedIndex index = new InvertedIndex();
        index.rebuild(Map.of(relevant, List.of("ca", "phe", "phe"), unrelated, List.of("bao", "tang")));
        BM25Scorer scorer = new BM25Scorer();

        assertTrue(scorer.score(index, relevant, List.of("phe")) > scorer.score(index, unrelated, List.of("phe")));
        assertEquals(2, index.termFrequency("phe", relevant));
    }
}
