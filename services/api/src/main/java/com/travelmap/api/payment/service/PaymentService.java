package com.travelmap.api.payment.service;

import com.travelmap.api.booking.model.BookingStatus;
import com.travelmap.api.booking.repository.BookingRepository;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.payment.dto.PaymentSessionResponse;
import com.travelmap.api.payment.model.PaymentEntity;
import com.travelmap.api.payment.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {
    private final BookingRepository bookingRepository; private final PaymentRepository paymentRepository;
    private final PaymentGateway gateway;
    public PaymentService(BookingRepository bookingRepository, PaymentRepository paymentRepository, PaymentGateway gateway) {
        this.bookingRepository=bookingRepository; this.paymentRepository=paymentRepository; this.gateway=gateway;
    }
    @Transactional
    public PaymentSessionResponse createSession(String email, UUID bookingId, String idempotencyKey) {
        var booking = bookingRepository.findByIdAndUser_EmailIgnoreCase(bookingId, email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking was not found"));
        if (booking.getStatus() != BookingStatus.PENDING)
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_STATE_INVALID", "Only pending booking can be paid");
        PaymentEntity payment = paymentRepository.findByBooking_Id(bookingId).orElseGet(() ->
                paymentRepository.save(new PaymentEntity(booking, gateway.name(), idempotencyKey, Instant.now())));
        if (payment.getPaymentUrl() == null) {
            var session = gateway.createSession(payment);
            payment.attachSession(session.externalTransactionId(), session.paymentUrl(), Instant.now());
        }
        return PaymentSessionResponse.from(payment);
    }
}
