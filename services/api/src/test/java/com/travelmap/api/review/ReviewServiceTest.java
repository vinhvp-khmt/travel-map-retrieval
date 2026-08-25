package com.travelmap.api.review;

import com.travelmap.api.auth.model.*;
import com.travelmap.api.booking.model.BookingEntity;
import com.travelmap.api.booking.repository.BookingRepository;
import com.travelmap.api.poi.model.*;
import com.travelmap.api.review.dto.ReviewRequest;
import com.travelmap.api.review.model.ReviewEntity;
import com.travelmap.api.review.repository.ReviewRepository;
import com.travelmap.api.review.service.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ReviewServiceTest {
    @Test void completedBookingCanBeReviewedOnceAndRefreshesRating() {
        var bookings = mock(BookingRepository.class); var reviews = mock(ReviewRepository.class);
        var ratings = mock(RatingAggregateService.class); var booking = completedBooking();
        when(bookings.findByIdAndUser_EmailIgnoreCase(booking.getId(), "user@example.com")).thenReturn(Optional.of(booking));
        when(reviews.save(any())).thenAnswer(call -> call.getArgument(0));
        var service = new ReviewService(bookings, reviews, new ReviewEligibilityService(reviews), ratings);
        var response = service.create("user@example.com", booking.getId(),
                new ReviewRequest(5, " Tuyệt vời ", List.of("https://example.com/photo.jpg")));
        assertEquals(5, response.rating()); assertEquals("Tuyệt vời", response.comment());
        assertEquals(1, response.imageUrls().size());
        verify(ratings).refresh(booking.getPoi().getId());
    }

    @Test void rejectsReviewUntilBookingIsCompleted() {
        var reviews = mock(ReviewRepository.class); var eligibility = new ReviewEligibilityService(reviews);
        assertThrows(com.travelmap.api.common.ApiException.class, () -> eligibility.requireEligible(pendingBooking()));
        verify(reviews, never()).existsByBooking_Id(any());
    }

    @Test void reviewCannotBeEditedAfterTwentyFourHours() {
        var now = Instant.parse("2026-08-24T00:00:00Z");
        var review = new ReviewEntity(completedBooking(), 4, "Good", List.of(), now);
        assertThrows(IllegalStateException.class,
                () -> review.edit(5, "Late", List.of(), now.plusSeconds(24 * 60 * 60)));
    }

    private static BookingEntity pendingBooking() {
        var user = new UserEntity("user@example.com", "hash", UserRole.USER);
        var owner = new UserEntity("owner@example.com", "hash", UserRole.OWNER);
        var poi = new PoiEntity(owner, new CategoryEntity(UUID.randomUUID(), "Cafe", "cafe"),
                "Cafe", "cafe", null, 10.77, 106.70, "Q1", 2, 10, true);
        poi.approve();
        return new BookingEntity(user, poi, Instant.now().plusSeconds(86400), 2, null, BigDecimal.TEN, Instant.now());
    }
    private static BookingEntity completedBooking() {
        var booking = pendingBooking(); var now = Instant.now(); booking.confirm(now); booking.complete(now); return booking;
    }
}
