package com.travelmap.api.review.controller;

import com.travelmap.api.review.dto.*;
import com.travelmap.api.review.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
public class ReviewController {
    private final ReviewService reviews;
    public ReviewController(ReviewService reviews) { this.reviews = reviews; }
    @PostMapping("/api/v1/bookings/{bookingId}/reviews") @ResponseStatus(HttpStatus.CREATED)
    ReviewResponse create(Authentication auth, @PathVariable UUID bookingId, @Valid @RequestBody ReviewRequest request) {
        return reviews.create(auth.getName(), bookingId, request);
    }
    @PatchMapping("/api/v1/reviews/{reviewId}")
    ReviewResponse update(Authentication auth, @PathVariable UUID reviewId, @Valid @RequestBody ReviewRequest request) {
        return reviews.update(auth.getName(), reviewId, request);
    }
    @DeleteMapping("/api/v1/reviews/{reviewId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(Authentication auth, @PathVariable UUID reviewId) { reviews.delete(auth.getName(), reviewId); }
    @PatchMapping("/api/v1/admin/reviews/{reviewId}/hide")
    ReviewResponse hide(@PathVariable UUID reviewId) { return reviews.hide(reviewId); }
}
