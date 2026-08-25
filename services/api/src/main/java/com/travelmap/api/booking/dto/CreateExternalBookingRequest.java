package com.travelmap.api.booking.dto;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public record CreateExternalBookingRequest(
        @NotBlank @Size(max = 160) String externalId,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 120) String category,
        @NotBlank @Size(max = 500) String address,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        @NotNull @Future OffsetDateTime visitAt,
        @Min(1) @Max(1000) int partySize,
        @Size(max = 1000) String notes
) { }
