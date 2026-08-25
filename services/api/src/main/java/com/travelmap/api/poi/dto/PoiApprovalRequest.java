package com.travelmap.api.poi.dto;

import com.travelmap.api.poi.model.ApprovalDecision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PoiApprovalRequest(
        @NotNull ApprovalDecision decision,
        @Size(max = 1000) String reason
) {
}
