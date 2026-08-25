package com.travelmap.api.payment.repository;

import com.travelmap.api.payment.model.PaymentEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {
    @EntityGraph(attributePaths = {"booking", "booking.user", "booking.poi"})
    Optional<PaymentEntity> findByBooking_Id(UUID bookingId);
    @EntityGraph(attributePaths = {"booking", "booking.user", "booking.poi"})
    Optional<PaymentEntity> findByIdAndBooking_User_EmailIgnoreCase(UUID id, String email);
}
