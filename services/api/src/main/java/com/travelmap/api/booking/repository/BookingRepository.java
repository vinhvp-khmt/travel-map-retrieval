package com.travelmap.api.booking.repository;

import com.travelmap.api.booking.model.BookingEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<BookingEntity, UUID> {
    @EntityGraph(attributePaths = {"poi", "poi.category"})
    Optional<BookingEntity> findByIdAndUser_EmailIgnoreCase(UUID id, String email);

    @Query(value = """
        SELECT COALESCE(SUM(b.party_size), 0) FROM booking b
        WHERE b.poi_id = :poiId
          AND b.visit_at < :slotEnd AND b.slot_end_at > :slotStart
          AND (b.status = 'CONFIRMED' OR (b.status = 'PENDING' AND b.hold_expires_at > :now))
        """, nativeQuery = true)
    int reservedCapacity(@Param("poiId") UUID poiId, @Param("slotStart") Instant slotStart,
                         @Param("slotEnd") Instant slotEnd, @Param("now") Instant now);

    @Modifying
    @Query("update BookingEntity b set b.status = com.travelmap.api.booking.model.BookingStatus.EXPIRED, " +
           "b.updatedAt = :now where b.status = com.travelmap.api.booking.model.BookingStatus.PENDING " +
           "and b.holdExpiresAt <= :now")
    int expireStale(@Param("now") Instant now);
}
