package com.travelmap.api.booking.service;

import com.travelmap.api.booking.dto.AvailabilityResponse;
import com.travelmap.api.booking.repository.BookingRepository;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiStatus;
import com.travelmap.api.poi.repository.PoiRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class AvailabilityService {
    private final PoiRepository poiRepository;
    private final BookingRepository bookingRepository;
    public AvailabilityService(PoiRepository poiRepository, BookingRepository bookingRepository) {
        this.poiRepository = poiRepository; this.bookingRepository = bookingRepository;
    }
    @Transactional(readOnly = true)
    public AvailabilityResponse check(UUID poiId, Instant visitAt) {
        PoiEntity poi = poiRepository.findByIdAndStatus(poiId, PoiStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POI_NOT_FOUND", "POI was not found"));
        validateBookable(poi, visitAt);
        Instant end = visitAt.plusSeconds(3600);
        int reserved = bookingRepository.reservedCapacity(poiId, visitAt, end, Instant.now());
        return new AvailabilityResponse(poiId, visitAt, end, poi.getCapacity(), reserved,
                Math.max(0, poi.getCapacity() - reserved));
    }
    public static void validateBookable(PoiEntity poi, Instant visitAt) {
        if (!poi.isBookingEnabled() || poi.getCapacity() == null)
            throw new ApiException(HttpStatus.CONFLICT, "POI_NOT_BOOKABLE", "POI does not accept bookings");
        if (!visitAt.isAfter(Instant.now()))
            throw new ApiException(HttpStatus.BAD_REQUEST, "VISIT_TIME_INVALID", "Visit time must be in the future");
    }
}
