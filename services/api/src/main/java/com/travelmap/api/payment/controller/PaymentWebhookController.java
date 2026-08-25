package com.travelmap.api.payment.controller;

import com.travelmap.api.payment.dto.PaymentWebhookRequest;
import com.travelmap.api.payment.service.PaymentWebhookService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {
    private final PaymentWebhookService service;
    public PaymentWebhookController(PaymentWebhookService service) { this.service=service; }
    @PostMapping("/{gateway}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void webhook(@PathVariable String gateway, @RequestHeader("X-TravelMap-Signature") String signature,
                 @Valid @RequestBody PaymentWebhookRequest request) {
        service.process(gateway, request, signature);
    }
}
