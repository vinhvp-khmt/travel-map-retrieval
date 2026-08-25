package com.travelmap.api.booking.service;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.booking.dto.*;
import com.travelmap.api.booking.model.BookingEntity;
import com.travelmap.api.booking.repository.BookingRepository;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.payment.model.PaymentStatus;
import com.travelmap.api.payment.repository.PaymentRepository;
import com.travelmap.api.poi.model.CategoryEntity;
import com.travelmap.api.poi.model.PoiEntity;
import com.travelmap.api.poi.model.PoiStatus;
import com.travelmap.api.poi.repository.CategoryRepository;
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
    private final CategoryRepository categoryRepository;
    public BookingService(BookingRepository bookingRepository, PoiRepository poiRepository, UserRepository userRepository,
                          PaymentRepository paymentRepository, DepositPolicy depositPolicy, CategoryRepository categoryRepository) {
        this.bookingRepository=bookingRepository; this.poiRepository=poiRepository; this.userRepository=userRepository;
        this.paymentRepository=paymentRepository; this.depositPolicy=depositPolicy; this.categoryRepository=categoryRepository;
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
    @Transactional
    public BookingResponse createExternal(String email, CreateExternalBookingRequest request) {
        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User was not found"));
        PoiEntity poi = poiRepository.findByExternalProviderAndExternalId("GEOAPIFY", request.externalId().trim())
                .orElseGet(() -> poiRepository.save(importExternalPoi(user, request)));
        return create(email, new CreateBookingRequest(poi.getId(), request.visitAt(), request.partySize(), request.notes()));
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
    private PoiEntity importExternalPoi(UserEntity owner, CreateExternalBookingRequest request) {
        CategoryEntity category = categoryRepository.findBySlug(categorySlug(request.category()))
                .orElseGet(() -> categoryRepository.findBySlug("tham-quan")
                        .orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "CATEGORY_NOT_CONFIGURED", "Default category is missing")));
        String name = trim(request.name());
        PoiEntity poi = new PoiEntity(owner, category, name, normalizeName(name),
                "Imported from Geoapify for booking demo", request.latitude(), request.longitude(), trim(request.address()),
                priceLevel(request.category()), 20, true);
        poi.attachExternalReference("GEOAPIFY", request.externalId().trim());
        poi.approve();
        return poi;
    }
    private static String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
    private static String categorySlug(String category) {
        String value = normalizeName(category);
        if (value.contains("cafe") || value.contains("coffee") || value.contains("cà phê")) return "ca-phe";
        if (value.contains("restaurant") || value.contains("food") || value.contains("nhà hàng")) return "nha-hang";
        if (value.contains("hotel") || value.contains("accommodation") || value.contains("lưu trú")) return "luu-tru";
        return "tham-quan";
    }
    private static Integer priceLevel(String category) {
        String value = normalizeName(category);
        if (value.contains("hotel") || value.contains("accommodation")) return 3;
        if (value.contains("restaurant")) return 2;
        return 1;
    }
}
