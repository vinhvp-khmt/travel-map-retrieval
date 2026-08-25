package com.travelmap.api.search.service;

import java.util.List;
import java.util.UUID;

public class BM25Scorer {
    private static final double K1 = 1.2;
    private static final double B = 0.75;

    public double score(InvertedIndex index, UUID documentId, List<String> queryTerms) {
        if (index.documentCount() == 0 || index.averageDocumentLength() == 0) return 0;
        double score = 0;
        for (String term : queryTerms.stream().distinct().toList()) {
            int tf = index.termFrequency(term, documentId);
            if (tf == 0) continue;
            int df = index.documentFrequency(term);
            double idf = Math.log(1.0 + (index.documentCount() - df + 0.5) / (df + 0.5));
            double lengthRatio = index.documentLength(documentId) / index.averageDocumentLength();
            score += idf * (tf * (K1 + 1.0)) / (tf + K1 * (1.0 - B + B * lengthRatio));
        }
        return score;
    }
}
