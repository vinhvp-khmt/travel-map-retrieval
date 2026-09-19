package com.travelmap.api.search.controller;

import com.travelmap.api.search.dto.SearchResponse;
import com.travelmap.api.search.model.SearchCriteria;
import com.travelmap.api.search.model.WeightProfile;
import com.travelmap.api.search.service.SearchService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {
    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponse search(@RequestParam("q") String query,
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
        return searchService.search(new SearchCriteria(query, latitude, longitude, radiusKm,
                visitAt, page, size, priceLevel, categoryId, WeightProfile.from(weightSource), diversify));
    }
}
