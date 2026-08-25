package com.travelmap.api.review.service;

import com.travelmap.api.booking.repository.BookingRepository;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.review.dto.*;
import com.travelmap.api.review.model.ReviewEntity;
import com.travelmap.api.review.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class ReviewService {
    private final BookingRepository bookings; private final ReviewRepository reviews;
    private final ReviewEligibilityService eligibility; private final RatingAggregateService ratings;
    public ReviewService(BookingRepository bookings, ReviewRepository reviews, ReviewEligibilityService eligibility,
                         RatingAggregateService ratings) {
        this.bookings = bookings; this.reviews = reviews; this.eligibility = eligibility; this.ratings = ratings;
    }
    @Transactional
    public ReviewResponse create(String email, UUID bookingId, ReviewRequest request) {
        var booking = bookings.findByIdAndUser_EmailIgnoreCase(bookingId, email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BOOKING_NOT_FOUND", "Booking was not found"));
        eligibility.requireEligible(booking);
        var review = reviews.save(new ReviewEntity(booking, request.rating(), request.comment(), request.imageUrls(), Instant.now()));
        reviews.flush();
        ratings.refresh(booking.getPoi().getId());
        return ReviewResponse.from(review);
    }
    @Transactional
    public ReviewResponse update(String email, UUID reviewId, ReviewRequest request) {
        var review = owned(reviewId, email);
        try { review.edit(request.rating(), request.comment(), request.imageUrls(), Instant.now()); }
        catch (IllegalStateException exception) { throw conflict(exception); }
        reviews.flush();
        ratings.refresh(review.getPoi().getId());
        return ReviewResponse.from(review);
    }
    @Transactional
    public void delete(String email, UUID reviewId) {
        var review = owned(reviewId, email);
        try { review.delete(Instant.now()); } catch (IllegalStateException exception) { throw conflict(exception); }
        reviews.flush();
        ratings.refresh(review.getPoi().getId());
    }
    @Transactional
    public ReviewResponse hide(UUID reviewId) {
        var review = reviews.findWithDetailsById(reviewId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "Review was not found"));
        try { review.hide(Instant.now()); } catch (IllegalStateException exception) { throw conflict(exception); }
        reviews.flush();
        ratings.refresh(review.getPoi().getId());
        return ReviewResponse.from(review);
    }
    private ReviewEntity owned(UUID id, String email) {
        return reviews.findByIdAndUser_EmailIgnoreCase(id, email)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "Review was not found"));
    }
    private ApiException conflict(IllegalStateException exception) {
        return new ApiException(HttpStatus.CONFLICT, "REVIEW_NOT_EDITABLE", exception.getMessage());
    }
}
