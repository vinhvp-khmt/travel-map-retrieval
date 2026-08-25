package com.travelmap.api.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "failed_window_started_at")
    private Instant failedWindowStartedAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    public UserEntity(String email, String passwordHash, UserRole role) {
        Instant now = Instant.now();
        this.id = UUID.randomUUID();
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.status = UserStatus.ACTIVE;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getFailedWindowStartedAt() { return failedWindowStartedAt; }
    public Instant getLockedUntil() { return lockedUntil; }

    public void recordFailedLogin(Instant now) {
        if (failedWindowStartedAt == null || failedWindowStartedAt.plusSeconds(900).isBefore(now)) {
            failedWindowStartedAt = now;
            failedAttempts = 1;
        } else {
            failedAttempts++;
        }
        if (failedAttempts >= 5) {
            lockedUntil = now.plusSeconds(900);
            failedAttempts = 0;
            failedWindowStartedAt = null;
        }
        updatedAt = now;
    }

    public void recordSuccessfulLogin(Instant now) {
        failedAttempts = 0;
        failedWindowStartedAt = null;
        lockedUntil = null;
        updatedAt = now;
    }

    public boolean isLocked(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }
}
