package com.travelmap.api.payment.controller;

import com.travelmap.api.payment.dto.PaymentSessionResponse;
import com.travelmap.api.payment.service.PaymentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService payments;
    public PaymentController(PaymentService payments) { this.payments = payments; }

    @PostMapping("/{paymentId}/mock-confirm")
    PaymentSessionResponse mockConfirm(Authentication auth, @PathVariable UUID paymentId) {
        return payments.confirmMockPayment(auth.getName(), paymentId);
    }
}
