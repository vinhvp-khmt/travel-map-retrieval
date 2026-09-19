package com.travelmap.api.search.controller;

import com.travelmap.api.search.dto.SearchResponse;
import com.travelmap.api.search.history.SearchHistoryItem;
import com.travelmap.api.search.history.SearchHistoryService;
import com.travelmap.api.search.model.SearchCriteria;
import com.travelmap.api.search.model.WeightProfile;
import com.travelmap.api.search.service.SearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private final SearchService searchService;
    private final SearchHistoryService searchHistoryService;

    public SearchController(SearchService searchService, SearchHistoryService searchHistoryService) {
        this.searchService = searchService;
        this.searchHistoryService = searchHistoryService;
    }

    @GetMapping
    public SearchResponse search(Authentication auth,
                                 @RequestParam("q") String query,
                                 @RequestParam double latitude,
                                 @RequestParam double longitude,
                                 @RequestParam(defaultValue = "2") double radiusKm,
                                 @RequestParam(required = false)
                                 @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime visitAt,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size,
                                 @RequestParam(required = false) Integer priceLevel,
                                 @RequestParam(required = false) UUID categoryId,
                                 @RequestParam(defaultValue = "v1") String profile,
                                 @RequestParam(required = false) String rankingMode,
                                 @RequestParam(defaultValue = "true") boolean diversify) {
        // rankingMode (keyword|distance|full) là tên tham số chuẩn cho việc so sánh
        // baseline (dataset/evaluation plan 2.9); profile (v1|v2) là tên cũ vẫn giữ để
        // tương thích ngược. Có cả hai thì rankingMode thắng.
        String weightSource = rankingMode != null ? rankingMode : profile;
        SearchCriteria criteria = new SearchCriteria(query, latitude, longitude, radiusKm, visitAt, page, size,
                priceLevel, categoryId, WeightProfile.from(weightSource), diversify);
        SearchResponse response = searchService.search(criteria);
        // Phase 6: /api/v1/search là public (permitAll), nên chỉ ghi lịch sử khi request
        // thực sự có user đăng nhập — không bao giờ làm hỏng một lượt search vì lý do này.
        searchHistoryService.recordIfAuthenticated(auth, criteria, response.normalizedQuery(), response.total());
        return response;
    }

    @GetMapping("/history")
    public List<SearchHistoryItem> history(Authentication auth, @RequestParam(defaultValue = "10") int limit) {
        return searchHistoryService.list(auth, limit);
    }

    @DeleteMapping("/history")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearHistory(Authentication auth) {
        searchHistoryService.deleteAll(auth);
    }

    @DeleteMapping("/history/{historyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHistoryItem(Authentication auth, @PathVariable UUID historyId) {
        searchHistoryService.delete(auth, historyId);
    }
}
