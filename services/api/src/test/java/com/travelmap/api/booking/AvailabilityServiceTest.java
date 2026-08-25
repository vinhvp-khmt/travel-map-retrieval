package com.travelmap.api.booking;

import com.travelmap.api.booking.repository.BookingRepository;
import com.travelmap.api.booking.service.AvailabilityService;
import com.travelmap.api.poi.model.PoiStatus;
import com.travelmap.api.poi.repository.PoiRepository;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AvailabilityServiceTest {
    @Test void subtractsLiveOverlappingReservations() {
        var pois=mock(PoiRepository.class); var bookings=mock(BookingRepository.class);
        var poi=BookingServiceTest.bookablePoi(10); Instant visit=Instant.now().plusSeconds(3600);
        when(pois.findByIdAndStatus(poi.getId(), PoiStatus.ACTIVE)).thenReturn(Optional.of(poi));
        when(bookings.reservedCapacity(eq(poi.getId()),eq(visit),eq(visit.plusSeconds(3600)),any())).thenReturn(6);
        var result=new AvailabilityService(pois,bookings).check(poi.getId(),visit);
        assertEquals(4,result.available());
    }
}
