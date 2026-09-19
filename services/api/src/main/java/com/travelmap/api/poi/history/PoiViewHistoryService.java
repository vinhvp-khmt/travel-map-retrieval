package com.travelmap.api.poi.history;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.poi.repository.PoiRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Phase 7 (IR search plan) — lịch sử POI đã xem của user. */
@Service
public class PoiViewHistoryService {
    private final PoiViewHistoryRepository repository;
    private final UserRepository userRepository;
    private final PoiRepository poiRepository;

    public PoiViewHistoryService(PoiViewHistoryRepository repository, UserRepository userRepository,
                                 PoiRepository poiRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.poiRepository = poiRepository;
    }

    @Transactional
    public void record(Authentication auth, UUID poiId) {
        if (!poiRepository.existsById(poiId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "POI_NOT_FOUND", "POI was not found");
        }
        repository.record(requireUserId(auth), poiId);
    }

    @Transactional(readOnly = true)
    public List<ViewedPoiItem> list(Authentication auth, int limit) {
        return repository.list(requireUserId(auth), limit);
    }

    @Transactional
    public void deleteAll(Authentication auth) {
        repository.deleteAll(requireUserId(auth));
    }

    @Transactional
    public void delete(Authentication auth, UUID poiId) {
        repository.delete(requireUserId(auth), poiId);
    }

    private UUID requireUserId(Authentication auth) {
        if (auth == null || auth.getName() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User was not found");
        }
        return userRepository.findFirstByEmailIgnoreCase(auth.getName())
                .map(UserEntity::getId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "User was not found"));
    }
}
