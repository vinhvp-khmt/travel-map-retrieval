package com.travelmap.api.payment.dto;

import com.travelmap.api.payment.model.PaymentEntity;
import java.math.BigDecimal;
import java.util.UUID;

public record PaymentSessionResponse(UUID paymentId, UUID bookingId, String gateway, String status,
                                     BigDecimal amount, String currency, String paymentUrl) {
    public static PaymentSessionResponse from(PaymentEntity payment) {
        return new PaymentSessionResponse(payment.getId(), payment.getBooking().getId(), payment.getGateway(),
                payment.getStatus().name(), payment.getAmount(), payment.getCurrency(), payment.getPaymentUrl());
    }
}
