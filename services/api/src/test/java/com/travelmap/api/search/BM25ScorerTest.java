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

    @Test
    void queryWithNoMatchingTermsScoresZero() {
        UUID doc = UUID.randomUUID();
        InvertedIndex index = new InvertedIndex();
        index.rebuild(Map.of(doc, List.of("ca", "phe")));
        BM25Scorer scorer = new BM25Scorer();

        assertEquals(0.0, scorer.score(index, doc, List.of("bao", "tang")));
    }

    @Test
    void shorterDocumentScoresAboveLongerDocumentWithSameTermFrequency() {
        // Chuẩn hoá theo document length (tham số b): cùng tf=1 nhưng doc dài hơn (nhiều
        // padding) phải bị phạt điểm nhẹ hơn — đúng ý nghĩa của length normalization.
        UUID shortDoc = UUID.randomUUID();
        UUID longDoc = UUID.randomUUID();
        InvertedIndex index = new InvertedIndex();
        index.rebuild(Map.of(
                shortDoc, List.of("ca", "phe"),
                longDoc, List.of("ca", "phe", "yen", "tinh", "cho", "hoc", "bai", "wifi", "on", "cap")));
        BM25Scorer scorer = new BM25Scorer();

        assertTrue(scorer.score(index, shortDoc, List.of("phe")) > scorer.score(index, longDoc, List.of("phe")));
    }

    @Test
    void multipleDocumentsRankByCombinedTermFrequency() {
        UUID best = UUID.randomUUID();
        UUID mid = UUID.randomUUID();
        UUID worst = UUID.randomUUID();
        InvertedIndex index = new InvertedIndex();
        index.rebuild(Map.of(
                best, List.of("ca", "phe", "ca", "phe", "hoc", "bai"),
                mid, List.of("ca", "phe", "yen", "tinh"),
                worst, List.of("nha", "hang", "buffet")));
        BM25Scorer scorer = new BM25Scorer();
        List<String> query = List.of("ca", "phe");

        double sBest = scorer.score(index, best, query);
        double sMid = scorer.score(index, mid, query);
        double sWorst = scorer.score(index, worst, query);
        assertTrue(sBest > sMid, "nhiều lần khớp từ khoá phải xếp trên ít lần khớp");
        assertTrue(sMid > sWorst, "khớp từ khoá phải xếp trên không khớp");
        assertEquals(0.0, sWorst);
    }

    @Test
    void customK1ChangesScoreComparedToDefault() {
        // k1 lấy từ config (constructor 2 tham số) phải thực sự đổi công thức, không
        // chỉ đổi tên biến — xác nhận phần "k1/b nằm trong config" của plan 1.3/2.3.
        UUID doc = UUID.randomUUID();
        InvertedIndex index = new InvertedIndex();
        index.rebuild(Map.of(doc, List.of("ca", "phe", "phe", "phe")));
        List<String> query = List.of("phe");

        BM25Scorer defaults = new BM25Scorer();
        BM25Scorer noSaturation = new BM25Scorer(1000.0, 0.75); // k1 rất lớn -> gần như tuyến tính theo tf

        assertTrue(noSaturation.score(index, doc, query) != defaults.score(index, doc, query));
    }

    @Test
    void customBChangesLengthNormalizationComparedToDefault() {
        // b chỉ có tác dụng khi documentLength lệch khỏi averageDocumentLength, nên cần
        // ít nhất 2 tài liệu độ dài khác nhau trong index mới quan sát được ảnh hưởng của b.
        UUID shortDoc = UUID.randomUUID();
        UUID longDoc = UUID.randomUUID();
        InvertedIndex index = new InvertedIndex();
        index.rebuild(Map.of(
                shortDoc, List.of("ca", "phe", "phe", "phe"),
                longDoc, List.of("ca", "phe", "yen", "tinh", "cho", "hoc", "bai", "wifi", "on", "cap")));
        List<String> query = List.of("phe");

        BM25Scorer defaults = new BM25Scorer();          // b = 0.75
        BM25Scorer noLengthNorm = new BM25Scorer(1.2, 0.0); // b = 0 -> tắt chuẩn hoá độ dài

        assertTrue(noLengthNorm.score(index, shortDoc, query) != defaults.score(index, shortDoc, query));
    }
}
