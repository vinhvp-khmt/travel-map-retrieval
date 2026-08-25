package com.travelmap.api.booking.dto;

import java.time.Instant;
import java.util.UUID;

public record AvailabilityResponse(UUID poiId, Instant visitAt, Instant slotEndAt,
                                   int capacity, int reserved, int available) { }
