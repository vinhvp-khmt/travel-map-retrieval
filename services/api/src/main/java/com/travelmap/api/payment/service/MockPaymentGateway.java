package com.travelmap.api.payment.service;

import com.travelmap.api.payment.model.PaymentEntity;
import org.springframework.stereotype.Component;

@Component
public class MockPaymentGateway implements PaymentGateway {
    public String name() { return "MOCK"; }
    public PaymentSession createSession(PaymentEntity payment) {
        String external = "mock_" + payment.getId();
        return new PaymentSession(external, "http://localhost:8080/mock-payments/" + payment.getId());
    }
}
