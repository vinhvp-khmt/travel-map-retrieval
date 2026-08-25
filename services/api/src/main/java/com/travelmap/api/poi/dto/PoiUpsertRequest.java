package com.travelmap.api.poi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record PoiUpsertRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(min = 3, max = 120) String name,
        @Size(max = 3000) String description,
        @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
        @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
        @NotBlank @Size(max = 500) String address,
        @Min(1) @Max(4) Integer priceLevel,
        @Positive Integer capacity,
        boolean bookingEnabled,
        @NotEmpty List<@Valid OpeningHourRequest> openingHours
) {
}
