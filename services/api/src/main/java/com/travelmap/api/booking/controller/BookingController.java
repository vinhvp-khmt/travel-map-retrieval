package com.travelmap.api.booking.controller;

import com.travelmap.api.booking.dto.*;
import com.travelmap.api.booking.service.BookingService;
import com.travelmap.api.payment.dto.PaymentSessionResponse;
import com.travelmap.api.payment.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @Validated @RequestMapping("/api/v1/bookings")
public class BookingController {
    private final BookingService bookings; private final PaymentService payments;
    public BookingController(BookingService bookings, PaymentService payments) { this.bookings=bookings; this.payments=payments; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    BookingResponse create(Authentication auth, @Valid @RequestBody CreateBookingRequest request) { return bookings.create(auth.getName(), request); }
    @GetMapping("/{id}") BookingResponse get(Authentication auth, @PathVariable UUID id) { return bookings.get(auth.getName(), id); }
    @PatchMapping("/{id}/cancel") BookingResponse cancel(Authentication auth, @PathVariable UUID id) { return bookings.cancel(auth.getName(), id); }
    @PostMapping("/{id}/payment-sessions") @ResponseStatus(HttpStatus.CREATED)
    PaymentSessionResponse paymentSession(Authentication auth, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") @Size(min=8,max=120) String key) {
        return payments.createSession(auth.getName(), id, key);
    }
}
