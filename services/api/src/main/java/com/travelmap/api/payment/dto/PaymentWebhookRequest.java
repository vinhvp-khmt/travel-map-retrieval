package com.travelmap.api.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PaymentWebhookRequest(@NotBlank @jakarta.validation.constraints.Size(max=120) String eventId,
                                    @NotNull UUID paymentId,
                                    @NotBlank @jakarta.validation.constraints.Pattern(regexp="PAID|FAILED") String status) {
    public String canonicalPayload() { return eventId + ":" + paymentId + ":" + status; }
}
