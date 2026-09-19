package com.travelmap.api.poi.history;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.poi.repository.PoiRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PoiViewHistoryServiceTest {

    private static final String EMAIL = "user@travelmap.local";

    @Test
    void record_ghiLuotXem_khiPoiTonTaiVaUserThat() {
        var repository = mock(PoiViewHistoryRepository.class);
        var users = mock(UserRepository.class);
        var pois = mock(PoiRepository.class);
        var user = new UserEntity(EMAIL, "hash", UserRole.USER);
        UUID poiId = UUID.randomUUID();
        when(users.findFirstByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(pois.existsById(poiId)).thenReturn(true);
        var service = new PoiViewHistoryService(repository, users, pois);

        service.record(authOf(EMAIL), poiId);

        verify(repository).record(user.getId(), poiId);
    }

    @Test
    void record_nemNotFound_khiPoiKhongTonTai() {
        var repository = mock(PoiViewHistoryRepository.class);
        var users = mock(UserRepository.class);
        var pois = mock(PoiRepository.class);
        UUID poiId = UUID.randomUUID();
        when(pois.existsById(poiId)).thenReturn(false);
        var service = new PoiViewHistoryService(repository, users, pois);

        assertThrows(ApiException.class, () -> service.record(authOf(EMAIL), poiId));
        verify(repository, never()).record(any(), any());
    }

    @Test
    void list_uyQuyenChoRepository_voiUserIdDaResolve() {
        var repository = mock(PoiViewHistoryRepository.class);
        var users = mock(UserRepository.class);
        var pois = mock(PoiRepository.class);
        var user = new UserEntity(EMAIL, "hash", UserRole.USER);
        when(users.findFirstByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        var service = new PoiViewHistoryService(repository, users, pois);

        service.list(authOf(EMAIL), 10);

        verify(repository).list(user.getId(), 10);
    }

    @Test
    void requireUserId_nemUnauthorized_khiAuthNull() {
        var repository = mock(PoiViewHistoryRepository.class);
        var users = mock(UserRepository.class);
        var pois = mock(PoiRepository.class);
        var service = new PoiViewHistoryService(repository, users, pois);

        assertThrows(ApiException.class, () -> service.list(null, 10));
    }

    private static Authentication authOf(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }
}
