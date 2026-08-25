package com.travelmap.api.payment.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "payment_webhook_event", uniqueConstraints = @UniqueConstraint(columnNames = {"gateway", "event_id"}))
public class PaymentWebhookEventEntity {
    @Id private UUID id;
    @Column(nullable = false, length = 30) private String gateway;
    @Column(name = "event_id", nullable = false, length = 120) private String eventId;
    @Column(name = "payload_hash", nullable = false, length = 64) private String payloadHash;
    @Column(name = "processed_at", nullable = false) private Instant processedAt;
    protected PaymentWebhookEventEntity() { }
    public PaymentWebhookEventEntity(String gateway, String eventId, String payloadHash, Instant now) {
        id = UUID.randomUUID(); this.gateway = gateway; this.eventId = eventId; this.payloadHash = payloadHash; processedAt = now;
    }
}
