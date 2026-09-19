package com.travelmap.api.search.history;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.search.model.SearchCriteria;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Phase 6 (IR search plan) — lịch sử tìm kiếm của user.
 *
 * <p>{@code GET /api/v1/search} là endpoint public ({@code permitAll}), nên
 * {@link #recordIfAuthenticated} chấp nhận {@code auth} null hoặc anonymous và chỉ ghi
 * lịch sử khi thực sự resolve được một user thật — im lặng bỏ qua ở mọi trường hợp khác,
 * không bao giờ làm hỏng một lượt search.
 */
@Service
public class SearchHistoryService {
    private final SearchHistoryRepository repository;
    private final UserRepository userRepository;

    public SearchHistoryService(SearchHistoryRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void recordIfAuthenticated(Authentication auth, SearchCriteria criteria, String normalizedQuery,
                                      int resultCount) {
        resolveUserId(auth).ifPresent(userId -> repository.record(userId, criteria.query(), normalizedQuery,
                criteria.latitude(), criteria.longitude(), criteria.radiusKm(), resultCount));
    }

    @Transactional(readOnly = true)
    public List<SearchHistoryItem> list(Authentication auth, int limit) {
        return repository.list(requireUserId(auth), limit);
    }

    @Transactional
    public void deleteAll(Authentication auth) {
        repository.deleteAll(requireUserId(auth));
    }

    @Transactional
    public void delete(Authentication auth, UUID historyId) {
        repository.delete(requireUserId(auth), historyId);
    }

    private Optional<UUID> resolveUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            return Optional.empty();
        }
        return userRepository.findFirstByEmailIgnoreCase(auth.getName()).map(UserEntity::getId);
    }

    private UUID requireUserId(Authentication auth) {
        return resolveUserId(auth).orElseThrow(
                () -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User was not found"));
    }
}
