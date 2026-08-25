package com.travelmap.api.review.model;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.booking.model.BookingEntity;
import com.travelmap.api.poi.model.PoiEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "review")
public class ReviewEntity {
    @Id private UUID id;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "booking_id") private BookingEntity booking;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "poi_id") private PoiEntity poi;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id") private UserEntity user;
    @Column(nullable = false) private int rating;
    @Column(length = 2000) private String comment;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private ReviewStatus status;
    @Column(name = "editable_until", nullable = false) private Instant editableUntil;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC") private List<ReviewImageEntity> images = new ArrayList<>();

    protected ReviewEntity() { }
    public ReviewEntity(BookingEntity booking, int rating, String comment, List<String> imageUrls, Instant now) {
        this.id = UUID.randomUUID(); this.booking = booking; this.poi = booking.getPoi(); this.user = booking.getUser();
        this.status = ReviewStatus.PUBLISHED; this.editableUntil = now.plusSeconds(24 * 60 * 60);
        this.createdAt = now; apply(rating, comment, imageUrls, now);
    }
    public void edit(int rating, String comment, List<String> imageUrls, Instant now) {
        requireEditable(now); apply(rating, comment, imageUrls, now);
    }
    public void delete(Instant now) { requireEditable(now); status = ReviewStatus.DELETED; updatedAt = now; }
    public void hide(Instant now) { if (status == ReviewStatus.DELETED) throw new IllegalStateException("Deleted review cannot be moderated"); status = ReviewStatus.HIDDEN; updatedAt = now; }
    private void apply(int rating, String comment, List<String> imageUrls, Instant now) {
        this.rating = rating; this.comment = comment == null || comment.isBlank() ? null : comment.trim();
        images.clear(); for (int i = 0; i < imageUrls.size(); i++) images.add(new ReviewImageEntity(this, imageUrls.get(i), i));
        updatedAt = now;
    }
    private void requireEditable(Instant now) {
        if (status != ReviewStatus.PUBLISHED) throw new IllegalStateException("Review is not editable");
        if (!now.isBefore(editableUntil)) throw new IllegalStateException("Review edit window has expired");
    }
    public UUID getId() { return id; } public BookingEntity getBooking() { return booking; }
    public PoiEntity getPoi() { return poi; } public UserEntity getUser() { return user; }
    public int getRating() { return rating; } public String getComment() { return comment; }
    public ReviewStatus getStatus() { return status; } public Instant getEditableUntil() { return editableUntil; }
    public Instant getCreatedAt() { return createdAt; } public List<ReviewImageEntity> getImages() { return List.copyOf(images); }
}
