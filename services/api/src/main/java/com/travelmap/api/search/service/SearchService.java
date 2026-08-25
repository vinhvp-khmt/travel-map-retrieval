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
import com.travelmap.api.search.repository.SearchLogRepository;
import com.travelmap.api.search.validation.SearchRequestValidator;
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
    private final PoiRepository poiRepository;
    private final CategoryRepository categoryRepository;
    private final SearchLogRepository searchLogRepository;
    private final VietnameseTokenizer tokenizer;
    private final QueryNormalizer queryNormalizer;
    private final SearchRequestValidator validator;
    private final TemporalFitService temporalFitService;
    private final RankingService rankingService;

    public SearchService(PoiRepository poiRepository, CategoryRepository categoryRepository,
                         SearchLogRepository searchLogRepository, VietnameseTokenizer tokenizer,
                         QueryNormalizer queryNormalizer, SearchRequestValidator validator,
                         TemporalFitService temporalFitService, RankingService rankingService) {
        this.poiRepository = poiRepository;
        this.categoryRepository = categoryRepository;
        this.searchLogRepository = searchLogRepository;
        this.tokenizer = tokenizer;
        this.queryNormalizer = queryNormalizer;
        this.validator = validator;
        this.temporalFitService = temporalFitService;
        this.rankingService = rankingService;
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
        BM25Scorer bm25Scorer = new BM25Scorer();

        List<SpatialCandidateProjection> spatial = poiRepository.findSpatialCandidates(
                criteria.latitude(), criteria.longitude(), criteria.radiusKm() * 1000,
                criteria.categoryId(), criteria.priceLevel(), CANDIDATE_LIMIT);
        Map<UUID, Double> distances = new HashMap<>();
        spatial.forEach(item -> distances.put(item.getId(), item.getDistanceMeters()));
        Map<UUID, PoiEntity> entities = new HashMap<>();
        poiRepository.findAllByIdIn(spatial.stream().map(SpatialCandidateProjection::getId).toList())
                .forEach(poi -> entities.put(poi.getId(), poi));

        Map<UUID, Double> rawScores = new HashMap<>();
        distances.keySet().forEach(id -> rawScores.put(id, bm25Scorer.score(index, id, queryTerms)));
        double maxBm25 = rawScores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        OffsetDateTime visitAt = criteria.visitAt() == null ? OffsetDateTime.now() : criteria.visitAt();
        double radiusMeters = criteria.radiusKm() * 1000;

        List<SearchResult> ranked = spatial.stream()
                .filter(candidate -> entities.containsKey(candidate.getId()))
                .filter(candidate -> rawScores.getOrDefault(candidate.getId(), 0.0) > 0)
                .map(candidate -> toResult(entities.get(candidate.getId()), candidate.getDistanceMeters(),
                        rawScores.getOrDefault(candidate.getId(), 0.0), maxBm25, radiusMeters, visitAt))
                .sorted((left, right) -> {
                    int scoreOrder = Double.compare(right.scoreDetail().finalScore(), left.scoreDetail().finalScore());
                    return scoreOrder != 0 ? scoreOrder : Boolean.compare(right.open(), left.open());
                })
                .toList();

        int from = Math.min(criteria.page() * criteria.size(), ranked.size());
        int to = Math.min(from + criteria.size(), ranked.size());
        List<SearchResult> page = ranked.subList(from, to);
        searchLogRepository.save(criteria, normalizedQuery, ranked.size());
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

    private SearchResult toResult(PoiEntity poi, double distance, double rawBm25, double maxBm25,
                                  double radiusMeters, OffsetDateTime visitAt) {
        TemporalFitService.TemporalFit temporal = temporalFitService.evaluate(poi, visitAt);
        ScoreDetail score = rankingService.score(rawBm25, maxBm25, distance, radiusMeters,
                temporal.score(), poi.getAvgRating().doubleValue());
        return new SearchResult(poi.getId(), poi.getName(), poi.getCategory().getName(), poi.getAddress(),
                poi.getLatitude(), poi.getLongitude(), Math.round(distance * 10.0) / 10.0, temporal.open(), score);
    }
}
