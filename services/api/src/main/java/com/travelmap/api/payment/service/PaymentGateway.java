package com.travelmap.api.payment.service;

import com.travelmap.api.payment.model.PaymentEntity;

public interface PaymentGateway {
    String name();
    PaymentSession createSession(PaymentEntity payment);
    record PaymentSession(String externalTransactionId, String paymentUrl) { }
}
