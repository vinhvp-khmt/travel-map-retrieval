package com.travelmap.api.booking.dto;

import com.travelmap.api.booking.model.BookingEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingResponse(UUID bookingId, UUID poiId, String poiName, Instant visitAt, Instant slotEndAt,
                              int partySize, String status, Instant holdExpiresAt, BigDecimal depositAmount,
                              String currency, boolean paymentRequired) {
    public static BookingResponse from(BookingEntity booking) {
        return new BookingResponse(booking.getId(), booking.getPoi().getId(), booking.getPoi().getName(),
                booking.getVisitAt(), booking.getSlotEndAt(), booking.getPartySize(), booking.getStatus().name(),
                booking.getHoldExpiresAt(), booking.getDepositAmount(), booking.getCurrency(),
                booking.getDepositAmount().signum() > 0);
    }
}
