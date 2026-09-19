package com.travelmap.api.poi.controller;

import com.travelmap.api.poi.dto.PoiResponse;
import com.travelmap.api.poi.history.PoiViewHistoryService;
import com.travelmap.api.poi.service.PoiService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/pois")
public class PoiController {
    private final PoiService poiService;
    private final PoiViewHistoryService poiViewHistoryService;

    public PoiController(PoiService poiService, PoiViewHistoryService poiViewHistoryService) {
        this.poiService = poiService;
        this.poiViewHistoryService = poiViewHistoryService;
    }

    @GetMapping("/{poiId}")
    PoiResponse getActive(@PathVariable UUID poiId) {
        return poiService.getActive(poiId);
    }

    /**
     * Phase 7 (IR search plan): ghi nhận user đã mở chi tiết POI này.
     *
     * <p>Frontend hiện render POI detail từ dữ liệu search result đã có sẵn (không gọi
     * {@link #getActive}), nên "mở POI detail" cần một tín hiệu riêng — gọi endpoint này
     * fire-and-forget ngay khi user chọn một POI. Không nằm trong {@code permitAll} nên
     * luôn yêu cầu đăng nhập (khớp {@code SecurityConfig.anyRequest().authenticated()}).
     */
    @PostMapping("/{poiId}/view")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void recordView(Authentication auth, @PathVariable UUID poiId) {
        poiViewHistoryService.record(auth, poiId);
    }
}
