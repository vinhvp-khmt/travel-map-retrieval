package com.travelmap.api.booking.dto;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID poiId,
        @NotNull @Future OffsetDateTime visitAt,
        @Min(1) @Max(1000) int partySize,
        @Size(max = 1000) String notes
) { }
