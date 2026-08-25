package com.travelmap.api.booking;

import com.travelmap.api.auth.model.*;
import com.travelmap.api.booking.model.*;
import com.travelmap.api.poi.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class BookingStateTest {
    @Test void pendingBookingConfirmsBeforeHoldExpiry() {
        Instant now=Instant.now(); BookingEntity booking=booking(now);
        booking.confirm(now.plusSeconds(60));
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertNull(booking.getHoldExpiresAt());
    }
    @Test void expiredHoldCannotConfirm() {
        Instant now=Instant.now(); BookingEntity booking=booking(now);
        assertThrows(IllegalStateException.class, () -> booking.confirm(now.plusSeconds(601)));
        assertEquals(BookingStatus.EXPIRED, booking.getStatus());
    }
    private static BookingEntity booking(Instant now) {
        var user=new UserEntity("user@example.com","hash",UserRole.USER);
        var poi=new PoiEntity(new UserEntity("owner@example.com","hash",UserRole.OWNER),
                new CategoryEntity(UUID.randomUUID(),"Cafe","cafe"),"Cafe","cafe",null,10.77,106.70,
                "Q1",2,10,true); poi.approve();
        return new BookingEntity(user,poi,now.plusSeconds(3600),2,null,new BigDecimal("40000.00"),now);
    }
}
