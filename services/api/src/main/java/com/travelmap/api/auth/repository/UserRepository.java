package com.travelmap.api.auth.repository;

import com.travelmap.api.auth.model.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<UserEntity> findByEmailIgnoreCase(String email);

    /**
     * Đọc user theo email KHÔNG khoá — dùng cho các chỗ chỉ cần resolve danh tính (ví dụ
     * lấy userId từ JWT để ghi/đọc lịch sử search/POI đã xem), không mutate app_user.
     * {@link #findByEmailIgnoreCase} dùng {@code SELECT ... FOR UPDATE} nên KHÔNG chạy được
     * trong transaction {@code readOnly = true} (Postgres từ chối); method này thì chạy được.
     */
    Optional<UserEntity> findFirstByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
