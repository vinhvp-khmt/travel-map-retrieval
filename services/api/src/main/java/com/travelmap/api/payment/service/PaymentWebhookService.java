package com.travelmap.api.payment.service;

import com.travelmap.api.common.ApiException;
import com.travelmap.api.payment.dto.PaymentWebhookRequest;
import com.travelmap.api.payment.model.PaymentWebhookEventEntity;
import com.travelmap.api.payment.repository.PaymentRepository;
import com.travelmap.api.payment.repository.PaymentWebhookEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class PaymentWebhookService {
    private final PaymentRepository payments; private final PaymentWebhookEventRepository events;
    private final byte[] secret;
    public PaymentWebhookService(PaymentRepository payments, PaymentWebhookEventRepository events,
            @Value("${travelmap.payment.webhook-secret}") String secret) {
        this.payments=payments; this.events=events; this.secret=secret.getBytes(StandardCharsets.UTF_8);
    }
    @Transactional
    public boolean process(String gateway, PaymentWebhookRequest request, String signature) {
        if (!validSignature(request.canonicalPayload(), signature))
            throw new ApiException(HttpStatus.UNAUTHORIZED, "PAYMENT_SIGNATURE_INVALID", "Webhook signature is invalid");
        String normalizedGateway = gateway.toUpperCase();
        if (events.existsByGatewayAndEventId(normalizedGateway, request.eventId())) return false;
        var payment = payments.findById(request.paymentId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PAYMENT_NOT_FOUND", "Payment was not found"));
        Instant now = Instant.now();
        try {
            if ("PAID".equals(request.status())) { payment.markPaid(now); payment.getBooking().confirm(now); }
            else payment.markFailed(now);
        } catch (IllegalStateException exception) {
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_STATE_INVALID", exception.getMessage());
        }
        events.save(new PaymentWebhookEventEntity(normalizedGateway, request.eventId(), sha256(request.canonicalPayload()), now));
        return true;
    }
    public String signForTesting(String payload) { return hmac(payload); }
    private boolean validSignature(String payload, String supplied) {
        if (supplied == null) return false;
        return MessageDigest.isEqual(hmac(payload).getBytes(StandardCharsets.US_ASCII),
                supplied.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }
    private String hmac(String payload) {
        try {
            Mac mac=Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(secret,"HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) { throw new IllegalStateException("Cannot calculate webhook HMAC", exception); }
    }
    private static String sha256(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
}
