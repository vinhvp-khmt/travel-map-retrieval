package com.travelmap.api.search.service;

import com.travelmap.api.common.ApiException;
import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiStatus;
import com.travelmap.api.poi.repository.CategoryRepository;
import com.travelmap.api.poi.repository.PoiRepository;
import com.travelmap.api.poi.repository.SpatialCandidateProjection;
import com.travelmap.api.search.dto.ScoreDetail;
import com.travelmap.api.search.dto.SearchResponse;
import com.travelmap.api.search.dto.SearchResult;
import com.travelmap.api.search.model.SearchCriteria;
import com.travelmap.api.search.model.WeightProfile;
import com.travelmap.api.search.repository.SearchLogRepository;
import com.travelmap.api.search.validation.SearchRequestValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SearchService {
    private static final int CANDIDATE_LIMIT = 500;
    /** Prior mặc định khi chưa có dữ liệu đánh giá nào để tính trung bình hệ thống. */
    private static final double DEFAULT_GLOBAL_MEAN_RATING = 3.7;

    private final PoiRepository poiRepository;
    private final CategoryRepository categoryRepository;
    private final SearchLogRepository searchLogRepository;
    private final VietnameseTokenizer tokenizer;
    private final QueryNormalizer queryNormalizer;
    private final SearchRequestValidator validator;
    private final TemporalFitService temporalFitService;
    private final RankingService rankingService;
    private final DiversityReranker diversityReranker;
    private final BM25Scorer bm25Scorer;

    /**
     * Constructor chính, được Spring dùng: {@code bm25Scorer} lấy từ context nên k1/b
     * đọc đúng config ({@code travelmap.search.bm25.*}) thay vì hằng số mặc định.
     */
    @Autowired
    public SearchService(PoiRepository poiRepository, CategoryRepository categoryRepository,
                         SearchLogRepository searchLogRepository, VietnameseTokenizer tokenizer,
                         QueryNormalizer queryNormalizer, SearchRequestValidator validator,
                         TemporalFitService temporalFitService, RankingService rankingService,
                         DiversityReranker diversityReranker, BM25Scorer bm25Scorer) {
        this.poiRepository = poiRepository;
        this.categoryRepository = categoryRepository;
        this.searchLogRepository = searchLogRepository;
        this.tokenizer = tokenizer;
        this.queryNormalizer = queryNormalizer;
        this.validator = validator;
        this.temporalFitService = temporalFitService;
        this.rankingService = rankingService;
        this.diversityReranker = diversityReranker;
        this.bm25Scorer = bm25Scorer;
    }

    /**
     * Constructor tương thích ngược 9 tham số (không có {@code bm25Scorer}): dùng
     * {@code BM25Scorer} mặc định (k1=1.2, b=0.75). Giữ lại để {@code SearchServiceTest} và
     * {@code IrEvaluationTest} — dựng {@code SearchService} bằng tay, không qua Spring —
     * tiếp tục biên dịch và chạy nguyên vẹn.
     */
    public SearchService(PoiRepository poiRepository, CategoryRepository categoryRepository,
                         SearchLogRepository searchLogRepository, VietnameseTokenizer tokenizer,
                         QueryNormalizer queryNormalizer, SearchRequestValidator validator,
                         TemporalFitService temporalFitService, RankingService rankingService,
                         DiversityReranker diversityReranker) {
        this(poiRepository, categoryRepository, searchLogRepository, tokenizer, queryNormalizer,
                validator, temporalFitService, rankingService, diversityReranker, new BM25Scorer());
    }

    @Transactional
    public SearchResponse search(SearchCriteria criteria) {
        validator.validate(criteria);
        if (criteria.categoryId() != null && !categoryRepository.existsById(criteria.categoryId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CATEGORY_NOT_FOUND", "Category does not exist");
        }
        String normalizedQuery = queryNormalizer.normalize(criteria.query());
        List<String> queryTerms = tokenizer.tokenize(criteria.query());
        List<PoiEntity> activePois = poiRepository.findAllByStatus(PoiStatus.ACTIVE);
        InvertedIndex index = buildIndex(activePois);

        // Prior cho công thức rating shrinkage (V2): điểm sao trung bình toàn hệ thống,
        // chỉ tính trên các quán đã có ít nhất một lượt đánh giá.
        double globalMeanRating = activePois.stream()
                .filter(poi -> poi.getRatingCount() > 0)
                .mapToDouble(poi -> poi.getAvgRating().doubleValue())
                .average()
                .orElse(DEFAULT_GLOBAL_MEAN_RATING);

        // Phần 2.4: GPS là tuỳ chọn. Có toạ độ thì dùng đúng đường cũ (PostGIS lọc + tính
        // khoảng cách thật). Không có thì lấy toàn bộ POI ACTIVE làm candidate (lọc thủ công
        // theo category/priceLevel cho khớp PostGIS), distance = null cho mọi kết quả —
        // RankingService sẽ tự dùng spatial score trung lập, search vẫn chạy bình thường.
        Map<UUID, Double> distances = new HashMap<>();
        Map<UUID, PoiEntity> entities = new HashMap<>();
        List<UUID> candidateIds;
        if (criteria.hasLocation()) {
            List<SpatialCandidateProjection> spatial = poiRepository.findSpatialCandidates(
                    criteria.latitude(), criteria.longitude(), criteria.radiusKm() * 1000,
                    criteria.categoryId(), criteria.priceLevel(), CANDIDATE_LIMIT);
            spatial.forEach(item -> distances.put(item.getId(), item.getDistanceMeters()));
            candidateIds = spatial.stream().map(SpatialCandidateProjection::getId).toList();
            poiRepository.findAllByIdIn(candidateIds).forEach(poi -> entities.put(poi.getId(), poi));
        } else {
            List<PoiEntity> withoutLocation = activePois.stream()
                    .filter(poi -> criteria.categoryId() == null || criteria.categoryId().equals(poi.getCategory().getId()))
                    .filter(poi -> criteria.priceLevel() == null || criteria.priceLevel().equals(poi.getPriceLevel()))
                    .limit(CANDIDATE_LIMIT)
                    .toList();
            withoutLocation.forEach(poi -> entities.put(poi.getId(), poi));
            candidateIds = withoutLocation.stream().map(PoiEntity::getId).toList();
        }

        Map<UUID, Double> rawScores = new HashMap<>();
        candidateIds.forEach(id -> rawScores.put(id, bm25Scorer.score(index, id, queryTerms)));
        double maxBm25 = rawScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        OffsetDateTime visitAt = criteria.visitAt() == null ? OffsetDateTime.now() : criteria.visitAt();
        double radiusMeters = criteria.radiusKm() * 1000;
        WeightProfile profile = criteria.profile();

        List<SearchResult> ranked = candidateIds.stream()
                .filter(entities::containsKey)
                .filter(id -> rawScores.getOrDefault(id, 0.0) > 0)
                .map(id -> toResult(entities.get(id), distances.get(id),
                        rawScores.getOrDefault(id, 0.0), maxBm25, radiusMeters, visitAt,
                        profile, globalMeanRating))
                .sorted((left, right) -> {
                    int scoreOrder = Double.compare(right.scoreDetail().finalScore(), left.scoreDetail().finalScore());
                    return scoreOrder != 0 ? scoreOrder : Boolean.compare(right.open(), left.open());
                })
                .toList();

        // Đa dạng hoá trang đầu (phần 2.3): gộp chi nhánh trùng tên + giới hạn số quán
        // cùng loại. Chạy SAU khi đã sort và TRƯỚC khi phân trang, nên chỉ đổi thứ tự
        // chứ không làm thay đổi tổng số kết quả.
        if (criteria.diversify()) {
            ranked = diversityReranker.rerank(ranked, DiversityReranker.DEFAULT_CATEGORY_CAP, true);
        }

        int from = Math.min(criteria.page() * criteria.size(), ranked.size());
        int to = Math.min(from + criteria.size(), ranked.size());
        List<SearchResult> page = ranked.subList(from, to);
        // search_log.latitude/longitude là NOT NULL (V3, log ẩn danh cho analytics) — bỏ
        // qua ghi log cho lượt search không GPS thay vì đổi schema hay NPE khi unbox.
        if (criteria.hasLocation()) {
            searchLogRepository.save(criteria, normalizedQuery, ranked.size());
        }
        String suggestion = ranked.isEmpty() ? "Try a broader radius or fewer filters" : null;
        return new SearchResponse(normalizedQuery, criteria.page(), criteria.size(), ranked.size(), page, suggestion);
    }

    private InvertedIndex buildIndex(List<PoiEntity> pois) {
        Map<UUID, List<String>> documents = new HashMap<>();
        for (PoiEntity poi : pois) {
            String text = poi.getName() + " " + (poi.getDescription() == null ? "" : poi.getDescription())
                    + " " + poi.getCategory().getName() + " " + poi.getAddress();
            documents.put(poi.getId(), tokenizer.tokenize(text));
        }
        InvertedIndex index = new InvertedIndex();
        index.rebuild(documents);
        return index;
    }

    private SearchResult toResult(PoiEntity poi, Double distance, double rawBm25, double maxBm25,
                                  double radiusMeters, OffsetDateTime visitAt,
                                  WeightProfile profile, double globalMeanRating) {
        TemporalFitService.TemporalFit temporal = temporalFitService.evaluate(poi, visitAt);
        ScoreDetail score = rankingService.score(profile, rawBm25, maxBm25, distance, radiusMeters,
                temporal.score(), poi.getAvgRating().doubleValue(), poi.getRatingCount(), globalMeanRating);
        Double roundedDistance = distance == null ? null : Math.round(distance * 10.0) / 10.0;
        return new SearchResult(poi.getId(), poi.getName(), poi.getCategory().getName(), poi.getAddress(),
                poi.getLatitude(), poi.getLongitude(), roundedDistance, temporal.open(), score);
    }
}
