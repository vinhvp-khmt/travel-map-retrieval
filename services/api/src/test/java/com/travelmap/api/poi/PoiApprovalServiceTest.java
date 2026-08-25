package com.travelmap.api.poi;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.poi.dto.PoiApprovalRequest;
import com.travelmap.api.poi.model.ApprovalDecision;
import com.travelmap.api.poi.model.CategoryEntity;
import com.travelmap.api.poi.model.PoiApprovalHistoryEntity;
import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.repository.PoiApprovalHistoryRepository;
import com.travelmap.api.poi.repository.PoiRepository;
import com.travelmap.api.poi.service.PoiApprovalService;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PoiApprovalServiceTest {
    @Test
    void adminApprovalActivatesPoiAndWritesAudit() {
        PoiRepository poiRepository = mock(PoiRepository.class);
        PoiApprovalHistoryRepository historyRepository = mock(PoiApprovalHistoryRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserEntity owner = new UserEntity("owner@example.com", "hash", UserRole.OWNER);
        UserEntity admin = new UserEntity("admin@example.com", "hash", UserRole.ADMIN);
        CategoryEntity category = new CategoryEntity(UUID.randomUUID(), "Cà phê", "ca-phe");
        PoiEntity poi = new PoiEntity(owner, category, "Quán A", "quan a", null,
                10.77, 106.69, "Quận 1", 2, 20, true);
        when(poiRepository.findById(poi.getId())).thenReturn(Optional.of(poi));
        when(userRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        PoiApprovalService service = new PoiApprovalService(poiRepository, historyRepository, userRepository);

        var response = service.decide("admin@example.com", poi.getId(),
                new PoiApprovalRequest(ApprovalDecision.APPROVED, null));

        assertEquals("ACTIVE", response.status());
        verify(historyRepository).save(any(PoiApprovalHistoryEntity.class));
    }

    @Test
    void rejectionRequiresReason() {
        PoiRepository poiRepository = mock(PoiRepository.class);
        PoiEntity poi = new PoiEntity(
                new UserEntity("owner@example.com", "hash", UserRole.OWNER),
                new CategoryEntity(UUID.randomUUID(), "Cà phê", "ca-phe"),
                "Quán A", "quan a", null, 10.77, 106.69, "Quận 1", 2, 20, true);
        when(poiRepository.findById(poi.getId())).thenReturn(Optional.of(poi));
        PoiApprovalService service = new PoiApprovalService(
                poiRepository, mock(PoiApprovalHistoryRepository.class), mock(UserRepository.class));

        ApiException exception = assertThrows(ApiException.class, () -> service.decide(
                "admin@example.com", poi.getId(), new PoiApprovalRequest(ApprovalDecision.REJECTED, " ")));
        assertEquals("REJECTION_REASON_REQUIRED", exception.getCode());
    }
}
