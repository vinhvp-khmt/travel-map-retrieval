package com.travelmap.api.booking.controller;

import com.travelmap.api.booking.dto.AvailabilityResponse;
import com.travelmap.api.booking.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/pois")
public class AvailabilityController {
    private final AvailabilityService service;
    public AvailabilityController(AvailabilityService service) { this.service=service; }
    @GetMapping("/{poiId}/availability")
    AvailabilityResponse check(@PathVariable UUID poiId,
            @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME) OffsetDateTime visitAt) {
        return service.check(poiId, visitAt.toInstant());
    }
}
