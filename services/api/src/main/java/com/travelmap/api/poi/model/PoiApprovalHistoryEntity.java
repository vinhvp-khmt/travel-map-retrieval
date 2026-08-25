package com.travelmap.api.poi.model;

import com.travelmap.api.auth.model.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "poi_approval_history")
public class PoiApprovalHistoryEntity {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poi_id") private PoiEntity poi;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "admin_id") private UserEntity admin;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private ApprovalDecision decision;
    @Column(length = 1000) private String reason;
    @Column(name = "created_at", nullable = false) private Instant createdAt;

    protected PoiApprovalHistoryEntity() { }
    public PoiApprovalHistoryEntity(PoiEntity poi, UserEntity admin, ApprovalDecision decision, String reason) {
        this.id = UUID.randomUUID();
        this.poi = poi;
        this.admin = admin;
        this.decision = decision;
        this.reason = reason;
        this.createdAt = Instant.now();
    }
}
