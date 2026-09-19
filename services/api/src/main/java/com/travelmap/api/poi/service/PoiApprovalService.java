package com.travelmap.api.poi.service;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.poi.dto.PoiApprovalRequest;
import com.travelmap.api.poi.dto.PoiResponse;
import com.travelmap.api.poi.model.ApprovalDecision;
import com.travelmap.api.poi.model.PoiApprovalHistoryEntity;
import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiStatus;
import com.travelmap.api.poi.repository.PoiApprovalHistoryRepository;
import com.travelmap.api.poi.repository.PoiRepository;
import com.travelmap.api.search.service.SearchIndexService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PoiApprovalService {
    private final PoiRepository poiRepository;
    private final PoiApprovalHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final SearchIndexService searchIndexService;

    public PoiApprovalService(PoiRepository poiRepository, PoiApprovalHistoryRepository historyRepository,
                              UserRepository userRepository, SearchIndexService searchIndexService) {
        this.poiRepository = poiRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.searchIndexService = searchIndexService;
    }

    @Transactional
    public PoiResponse decide(String adminEmail, UUID poiId, PoiApprovalRequest request) {
        PoiEntity poi = poiRepository.findById(poiId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POI_NOT_FOUND", "POI was not found"));
        if (poi.getStatus() != PoiStatus.PENDING_APPROVAL) {
            throw new ApiException(HttpStatus.CONFLICT, "POI_NOT_PENDING", "Only pending POIs can be approved or rejected");
        }
        String reason = request.reason() == null ? null : request.reason().trim();
        if (request.decision() == ApprovalDecision.REJECTED && (reason == null || reason.isBlank())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REJECTION_REASON_REQUIRED", "A rejection reason is required");
        }
        UserEntity admin = userRepository.findByEmailIgnoreCase(adminEmail)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "Admin user was not found"));
        if (request.decision() == ApprovalDecision.APPROVED) poi.approve(); else poi.reject();
        historyRepository.save(new PoiApprovalHistoryEntity(poi, admin, request.decision(), reason));
        poiRepository.flush();
        searchIndexService.rebuild();
        return PoiResponse.from(poi);
    }
}
