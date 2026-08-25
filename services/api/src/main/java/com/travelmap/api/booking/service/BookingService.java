package com.travelmap.api.booking.service;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.booking.dto.*;
import com.travelmap.api.booking.model.BookingEntity;
import com.travelmap.api.booking.repository.BookingRepository;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.payment.model.PaymentStatus;
import com.travelmap.api.payment.repository.PaymentRepository;
import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiStatus;
import com.travelmap.api.poi.repository.PoiRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class BookingService {
    private final BookingRepository bookingRepository; private final PoiRepository poiRepository;
    private final UserRepository userRepository; private final PaymentRepository paymentRepository;
    private final DepositPolicy depositPolicy;
    public BookingService(BookingRepository bookingRepository, PoiRepository poiRepository, UserRepository userRepository,
                          PaymentRepository paymentRepository, DepositPolicy depositPolicy) {
        this.bookingRepository=bookingRepository; this.poiRepository=poiRepository; this.userRepository=userRepository;
        this.paymentRepository=paymentRepository; this.depositPolicy=depositPolicy;
    }
    @Transactional
    public BookingResponse create(String email, CreateBookingRequest request) {
        Instant now = Instant.now(); Instant visitAt = request.visitAt().toInstant();
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User was not found"));
        PoiEntity poi = poiRepository.findByIdForUpdate(request.poiId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "POI_NOT_FOUND", "POI was not found"));
        if (poi.getStatus() != PoiStatus.ACTIVE) throw new ApiException(HttpStatus.CONFLICT, "POI_NOT_ACTIVE", "POI is not active");
        AvailabilityService.validateBookable(poi, visitAt);
        int reserved = bookingRepository.reservedCapacity(poi.getId(), visitAt, visitAt.plusSeconds(3600), now);
        if (request.partySize() > poi.getCapacity() - reserved)
            throw new ApiException(HttpStatus.CONFLICT, "CAPACITY_UNAVAILABLE", "Requested capacity is unavailable");
        BookingEntity booking = new BookingEntity(user, poi, visitAt, request.partySize(), trim(request.notes()),
                depositPolicy.calculate(poi, request.partySize()), now);
        return BookingResponse.from(bookingRepository.save(booking));
    }
    @Transactional(readOnly = true)
    public BookingResponse get(String email, UUID id) { return BookingResponse.from(requireOwned(email, id)); }
    @Transactional
    public BookingResponse cancel(String email, UUID id) {
        BookingEntity booking = requireOwned(email, id); Instant now = Instant.now();
        paymentRepository.findByBooking_Id(id).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PAID && booking.getVisitAt().minusSeconds(7200).isAfter(now)) payment.refund(now);
        });
        try { booking.cancel(now); }
        catch (IllegalStateException e) { throw new ApiException(HttpStatus.CONFLICT, "BOOKING_STATE_INVALID", e.getMessage()); }
        return BookingResponse.from(booking);
    }
    private BookingEntity requireOwned(String email, UUID id) {
        return bookingRepository.findByIdAndUser_EmailIgnoreCase(id, email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking was not found"));
    }
    private static String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
