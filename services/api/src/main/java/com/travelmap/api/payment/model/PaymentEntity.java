package com.travelmap.api.payment.model;

import com.travelmap.api.booking.model.BookingEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "payment")
public class PaymentEntity {
    @Id private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id") private BookingEntity booking;
    @Column(nullable = false, length = 30) private String gateway;
    @Column(name = "external_txn_id", length = 120) private String externalTxnId;
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 120) private String idempotencyKey;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private PaymentStatus status;
    @Column(name = "payment_url", length = 500) private String paymentUrl;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected PaymentEntity() { }
    public PaymentEntity(BookingEntity booking, String gateway, String idempotencyKey, Instant now) {
        this.id = UUID.randomUUID(); this.booking = booking; this.gateway = gateway;
        this.idempotencyKey = idempotencyKey; this.amount = booking.getDepositAmount(); this.currency = booking.getCurrency();
        this.status = PaymentStatus.CREATED; this.createdAt = now; this.updatedAt = now;
    }
    public void attachSession(String externalTxnId, String paymentUrl, Instant now) {
        this.externalTxnId = externalTxnId; this.paymentUrl = paymentUrl; this.updatedAt = now;
    }
    public void markPaid(Instant now) { if (status != PaymentStatus.PAID) { status = PaymentStatus.PAID; paidAt = now; updatedAt = now; } }
    public void markFailed(Instant now) { if (status == PaymentStatus.CREATED) { status = PaymentStatus.FAILED; updatedAt = now; } }
    public void refund(Instant now) { if (status != PaymentStatus.PAID) throw new IllegalStateException("Only paid payment can be refunded"); status = PaymentStatus.REFUNDED; updatedAt = now; }
    public UUID getId(){return id;} public BookingEntity getBooking(){return booking;} public String getGateway(){return gateway;}
    public String getExternalTxnId(){return externalTxnId;} public String getIdempotencyKey(){return idempotencyKey;}
    public BigDecimal getAmount(){return amount;} public String getCurrency(){return currency;} public PaymentStatus getStatus(){return status;}
    public String getPaymentUrl(){return paymentUrl;} public Instant getPaidAt(){return paidAt;}
}
