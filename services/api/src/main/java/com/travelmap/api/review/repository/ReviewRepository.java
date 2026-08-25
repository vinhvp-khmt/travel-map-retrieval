package com.travelmap.api.review.repository;

import com.travelmap.api.review.model.ReviewEntity;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {
    boolean existsByBooking_Id(UUID bookingId);
    @EntityGraph(attributePaths = {"booking", "poi", "user", "images"})
    Optional<ReviewEntity> findByIdAndUser_EmailIgnoreCase(UUID id, String email);
    @EntityGraph(attributePaths = {"booking", "poi", "user", "images"})
    Optional<ReviewEntity> findWithDetailsById(UUID id);
}
