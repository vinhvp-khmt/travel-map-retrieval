package com.travelmap.api.poi.controller;

import com.travelmap.api.poi.dto.PoiApprovalRequest;
import com.travelmap.api.poi.dto.PoiResponse;
import com.travelmap.api.poi.service.PoiApprovalService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/pois")
public class AdminPoiController {
    private final PoiApprovalService approvalService;

    public AdminPoiController(PoiApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PatchMapping("/{poiId}/approval")
    PoiResponse decide(Authentication authentication, @PathVariable UUID poiId,
                       @Valid @RequestBody PoiApprovalRequest request) {
        return approvalService.decide(authentication.getName(), poiId, request);
    }
}
