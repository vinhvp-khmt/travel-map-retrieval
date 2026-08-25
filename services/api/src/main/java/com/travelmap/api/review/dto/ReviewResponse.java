package com.travelmap.api.review.dto;

import com.travelmap.api.review.model.ReviewEntity;
import java.time.Instant;
import java.util.*;

public record ReviewResponse(UUID id, UUID bookingId, UUID poiId, int rating, String comment,
                             String status, List<String> imageUrls, Instant editableUntil, Instant createdAt) {
    public static ReviewResponse from(ReviewEntity review) {
        return new ReviewResponse(review.getId(), review.getBooking().getId(), review.getPoi().getId(),
                review.getRating(), review.getComment(), review.getStatus().name(),
                review.getImages().stream().map(image -> image.getUrl()).toList(),
                review.getEditableUntil(), review.getCreatedAt());
    }
}
