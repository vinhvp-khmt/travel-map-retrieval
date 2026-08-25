package com.travelmap.api.poi.repository;

import com.travelmap.api.poi.model.PoiApprovalHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PoiApprovalHistoryRepository extends JpaRepository<PoiApprovalHistoryEntity, UUID> {
}
