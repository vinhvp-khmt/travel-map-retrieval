package com.travelmap.api.poi.controller;

import com.travelmap.api.poi.history.PoiViewHistoryService;
import com.travelmap.api.poi.history.ViewedPoiItem;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Phase 7 (IR search plan): "Địa điểm đã xem" của user hiện tại. */
@RestController
@RequestMapping("/api/v1/users/me")
public class UserActivityController {
    private final PoiViewHistoryService poiViewHistoryService;

    public UserActivityController(PoiViewHistoryService poiViewHistoryService) {
        this.poiViewHistoryService = poiViewHistoryService;
    }

    @GetMapping("/viewed-pois")
    public List<ViewedPoiItem> viewedPois(Authentication auth, @RequestParam(defaultValue = "10") int limit) {
        return poiViewHistoryService.list(auth, limit);
    }

    @DeleteMapping("/viewed-pois")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearViewedPois(Authentication auth) {
        poiViewHistoryService.deleteAll(auth);
    }

    @DeleteMapping("/viewed-pois/{poiId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeViewedPoi(Authentication auth, @PathVariable UUID poiId) {
        poiViewHistoryService.delete(auth, poiId);
    }
}
