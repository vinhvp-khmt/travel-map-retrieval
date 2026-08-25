package com.travelmap.api.review.service;

import com.travelmap.api.booking.model.*;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.review.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ReviewEligibilityService {
    private final ReviewRepository reviews;
    public ReviewEligibilityService(ReviewRepository reviews) { this.reviews = reviews; }
    public void requireEligible(BookingEntity booking) {
        if (booking.getStatus() != BookingStatus.COMPLETED)
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_NOT_COMPLETED", "Only a completed booking can be reviewed");
        if (reviews.existsByBooking_Id(booking.getId()))
            throw new ApiException(HttpStatus.CONFLICT, "BOOKING_ALREADY_REVIEWED", "This booking already has a review");
    }
}
