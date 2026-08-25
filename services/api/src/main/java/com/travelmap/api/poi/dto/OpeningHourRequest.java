package com.travelmap.api.poi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalTime;

public record OpeningHourRequest(
        @Min(1) @Max(7) int dayOfWeek,
        LocalTime openTime,
        LocalTime closeTime,
        boolean closed
) {
}
