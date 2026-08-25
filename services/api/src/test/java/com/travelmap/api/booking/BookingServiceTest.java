package com.travelmap.api.booking;

import com.travelmap.api.auth.model.*;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.booking.dto.CreateBookingRequest;
import com.travelmap.api.booking.model.BookingEntity;
import com.travelmap.api.booking.repository.BookingRepository;
import com.travelmap.api.booking.service.*;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.payment.repository.PaymentRepository;
import com.travelmap.api.poi.model.*;
import com.travelmap.api.poi.repository.PoiRepository;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BookingServiceTest {
    @Test void createsTenMinuteHoldWhenCapacityExists() {
        var bookings=mock(BookingRepository.class); var pois=mock(PoiRepository.class);
        var users=mock(UserRepository.class); var payments=mock(PaymentRepository.class);
        var user=new UserEntity("user@example.com","hash",UserRole.USER); var poi=bookablePoi(5);
        when(users.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(pois.findByIdForUpdate(poi.getId())).thenReturn(Optional.of(poi));
        when(bookings.reservedCapacity(eq(poi.getId()),any(),any(),any())).thenReturn(2);
        when(bookings.save(any())).thenAnswer(call -> call.getArgument(0));
        var service=new BookingService(bookings,pois,users,payments,new DepositPolicy());
        var response=service.create("user@example.com",new CreateBookingRequest(poi.getId(),OffsetDateTime.now().plusDays(1),3," window "));
        assertEquals("PENDING",response.status()); assertEquals(3,response.partySize());
        assertNotNull(response.holdExpiresAt()); assertTrue(response.paymentRequired());
        verify(pois).findByIdForUpdate(poi.getId());
    }
    @Test void rejectsRequestAboveRemainingCapacity() {
        var bookings=mock(BookingRepository.class); var pois=mock(PoiRepository.class);
        var users=mock(UserRepository.class); var payments=mock(PaymentRepository.class);
        var user=new UserEntity("user@example.com","hash",UserRole.USER); var poi=bookablePoi(5);
        when(users.findByEmailIgnoreCase(any())).thenReturn(Optional.of(user));
        when(pois.findByIdForUpdate(poi.getId())).thenReturn(Optional.of(poi));
        when(bookings.reservedCapacity(eq(poi.getId()),any(),any(),any())).thenReturn(4);
        var service=new BookingService(bookings,pois,users,payments,new DepositPolicy());
        assertThrows(ApiException.class,()->service.create("user@example.com",
                new CreateBookingRequest(poi.getId(),OffsetDateTime.now().plusDays(1),2,null)));
        verify(bookings,never()).save(any(BookingEntity.class));
    }
    static PoiEntity bookablePoi(int capacity) {
        var poi=new PoiEntity(new UserEntity("owner@example.com","hash",UserRole.OWNER),
                new CategoryEntity(UUID.randomUUID(),"Cafe","cafe"),"Cafe","cafe",null,10.77,106.70,
                "Q1",2,capacity,true); poi.approve(); return poi;
    }
}
