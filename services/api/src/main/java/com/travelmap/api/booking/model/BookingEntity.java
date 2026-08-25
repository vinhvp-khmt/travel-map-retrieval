package com.travelmap.api.booking.model;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.poi.model.PoiEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking")
public class BookingEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "poi_id") private PoiEntity poi;
    @Column(name = "visit_at", nullable = false) private Instant visitAt;
    @Column(name = "slot_end_at", nullable = false) private Instant slotEndAt;
    @Column(name = "party_size", nullable = false) private int partySize;
    @Column(length = 1000) private String notes;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private BookingStatus status;
    @Column(name = "hold_expires_at") private Instant holdExpiresAt;
    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2) private BigDecimal depositAmount;
    @Column(nullable = false, length = 3) private String currency;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected BookingEntity() { }

    public BookingEntity(UserEntity user, PoiEntity poi, Instant visitAt, int partySize, String notes,
                         BigDecimal depositAmount, Instant now) {
        this.id = UUID.randomUUID(); this.user = user; this.poi = poi; this.visitAt = visitAt;
        this.slotEndAt = visitAt.plusSeconds(3600); this.partySize = partySize; this.notes = notes;
        this.depositAmount = depositAmount; this.currency = "VND"; this.status = BookingStatus.PENDING;
        this.holdExpiresAt = now.plusSeconds(600); this.createdAt = now; this.updatedAt = now;
    }

    public void confirm(Instant now) {
        require(BookingStatus.PENDING);
        if (!holdExpiresAt.isAfter(now)) { expire(now); throw new IllegalStateException("Booking hold has expired"); }
        status = BookingStatus.CONFIRMED; holdExpiresAt = null; updatedAt = now;
    }
    public void expire(Instant now) { require(BookingStatus.PENDING); status = BookingStatus.EXPIRED; updatedAt = now; }
    public void cancel(Instant now) {
        if (status != BookingStatus.PENDING && status != BookingStatus.CONFIRMED)
            throw new IllegalStateException("Booking cannot be cancelled from " + status);
        status = BookingStatus.CANCELLED; holdExpiresAt = null; updatedAt = now;
    }
    public void complete(Instant now) { require(BookingStatus.CONFIRMED); status = BookingStatus.COMPLETED; updatedAt = now; }
    private void require(BookingStatus expected) {
        if (status != expected) throw new IllegalStateException("Expected " + expected + " but was " + status);
    }

    public UUID getId() { return id; } public UserEntity getUser() { return user; }
    public PoiEntity getPoi() { return poi; } public Instant getVisitAt() { return visitAt; }
    public Instant getSlotEndAt() { return slotEndAt; } public int getPartySize() { return partySize; }
    public String getNotes() { return notes; } public BookingStatus getStatus() { return status; }
    public Instant getHoldExpiresAt() { return holdExpiresAt; } public BigDecimal getDepositAmount() { return depositAmount; }
    public String getCurrency() { return currency; }
}
