package com.travelmap.api.payment;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.booking.model.BookingEntity;
import com.travelmap.api.booking.model.BookingStatus;
import com.travelmap.api.booking.repository.BookingRepository;
import com.travelmap.api.payment.model.PaymentEntity;
import com.travelmap.api.payment.model.PaymentStatus;
import com.travelmap.api.payment.repository.PaymentRepository;
import com.travelmap.api.payment.service.PaymentGateway;
import com.travelmap.api.payment.service.PaymentService;
import com.travelmap.api.poi.model.CategoryEntity;
import com.travelmap.api.poi.model.PoiEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentServiceTest {
    @Test void mockConfirmMarksPaymentPaidAndBookingConfirmed() {
        var bookings = mock(BookingRepository.class);
        var payments = mock(PaymentRepository.class);
        PaymentEntity payment = payment();
        when(payments.findByIdAndBooking_User_EmailIgnoreCase(payment.getId(), "user@example.com")).thenReturn(Optional.of(payment));
        var service = new PaymentService(bookings, payments, mock(PaymentGateway.class));

        var response = service.confirmMockPayment("user@example.com", payment.getId());

        assertEquals("PAID", response.status());
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(BookingStatus.CONFIRMED, payment.getBooking().getStatus());
    }

    private static PaymentEntity payment() {
        Instant now = Instant.now();
        var user = new UserEntity("user@example.com", "hash", UserRole.USER);
        var poi = new PoiEntity(new UserEntity("owner@example.com", "hash", UserRole.OWNER),
                new CategoryEntity(UUID.randomUUID(), "Cafe", "cafe"), "Cafe", "cafe", null,
                10.77, 106.70, "Q1", 2, 10, true);
        poi.approve();
        var booking = new BookingEntity(user, poi, now.plusSeconds(3600), 2, null, new BigDecimal("40000"), now);
        return new PaymentEntity(booking, "MOCK", "idem-123456", now);
    }
}
