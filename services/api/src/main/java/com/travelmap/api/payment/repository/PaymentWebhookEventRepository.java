package com.travelmap.api.payment.repository;

import com.travelmap.api.payment.model.PaymentWebhookEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PaymentWebhookEventRepository extends JpaRepository<PaymentWebhookEventEntity, UUID> {
    boolean existsByGatewayAndEventId(String gateway, String eventId);
}
