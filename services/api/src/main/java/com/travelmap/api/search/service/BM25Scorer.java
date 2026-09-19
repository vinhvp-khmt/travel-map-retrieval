package com.travelmap.api.search.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * BM25 tự implement (Robertson IDF + TF saturation theo document length).
 *
 * <p>{@code k1} và {@code b} nằm trong config ({@code travelmap.search.bm25.k1/b}, mặc định
 * 1.2/0.75 — giá trị chuẩn trong tài liệu IR) để có thể đổi khi chạy experiment mà không cần
 * sửa code, đúng yêu cầu plan 1.3/2.3. {@code @Component} để Spring tiêm theo config; constructor
 * không tham số vẫn giữ để không phá test/lời gọi cũ đang dùng {@code new BM25Scorer()}.
 */
@Component
public class BM25Scorer {
    private static final double DEFAULT_K1 = 1.2;
    private static final double DEFAULT_B = 0.75;

    private final double k1;
    private final double b;

    public BM25Scorer() {
        this(DEFAULT_K1, DEFAULT_B);
    }

    @Autowired
    public BM25Scorer(
            @Value("${travelmap.search.bm25.k1:1.2}") double k1,
            @Value("${travelmap.search.bm25.b:0.75}") double b) {
        this.k1 = k1;
        this.b = b;
    }

    public double score(InvertedIndex index, UUID documentId, List<String> queryTerms) {
        if (index.documentCount() == 0 || index.averageDocumentLength() == 0) return 0;
        double score = 0;
        for (String term : queryTerms.stream().distinct().toList()) {
            int tf = index.termFrequency(term, documentId);
            if (tf == 0) continue;
            int df = index.documentFrequency(term);
            double idf = Math.log(1.0 + (index.documentCount() - df + 0.5) / (df + 0.5));
            double lengthRatio = index.documentLength(documentId) / index.averageDocumentLength();
            score += idf * (tf * (k1 + 1.0)) / (tf + k1 * (1.0 - b + b * lengthRatio));
        }
        return score;
    }
}
