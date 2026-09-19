package com.travelmap.api.search.history;

import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.common.ApiException;
import com.travelmap.api.search.model.SearchCriteria;
import com.travelmap.api.search.model.WeightProfile;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SearchHistoryServiceTest {

    private static final String EMAIL = "user@travelmap.local";
    private static final SearchCriteria CRITERIA = new SearchCriteria(
            "cà phê học bài", 10.7769, 106.7009, 10.0, null, 0, 20, null, null, WeightProfile.V1, true);

    @Test
    void recordIfAuthenticated_ghiLichSu_khiCoUserThat() {
        var repository = mock(SearchHistoryRepository.class);
        var users = mock(UserRepository.class);
        var user = new UserEntity(EMAIL, "hash", UserRole.USER);
        when(users.findFirstByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        var service = new SearchHistoryService(repository, users);

        service.recordIfAuthenticated(authOf(EMAIL), CRITERIA, "ca phe hoc bai", 5);

        verify(repository).record(eq(user.getId()), eq("cà phê học bài"), eq("ca phe hoc bai"),
                eq(10.7769), eq(106.7009), eq(10.0), eq(5));
    }

    @Test
    void recordIfAuthenticated_boQua_khiAuthNull() {
        var repository = mock(SearchHistoryRepository.class);
        var users = mock(UserRepository.class);
        var service = new SearchHistoryService(repository, users);

        service.recordIfAuthenticated(null, CRITERIA, "ca phe hoc bai", 5);

        verify(repository, never()).record(any(), anyString(), anyString(), anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    void recordIfAuthenticated_boQua_khiAnonymous() {
        var repository = mock(SearchHistoryRepository.class);
        var users = mock(UserRepository.class);
        when(users.findFirstByEmailIgnoreCase("anonymousUser")).thenReturn(Optional.empty());
        var service = new SearchHistoryService(repository, users);
        Authentication anonymous = new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        service.recordIfAuthenticated(anonymous, CRITERIA, "ca phe hoc bai", 5);

        verify(repository, never()).record(any(), anyString(), anyString(), anyDouble(), anyDouble(), anyDouble(), anyInt());
    }

    @Test
    void list_uyQuyenChoRepository_voiUserIdDaResolve() {
        var repository = mock(SearchHistoryRepository.class);
        var users = mock(UserRepository.class);
        var user = new UserEntity(EMAIL, "hash", UserRole.USER);
        when(users.findFirstByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        var service = new SearchHistoryService(repository, users);

        service.list(authOf(EMAIL), 10);

        verify(repository).list(user.getId(), 10);
    }

    @Test
    void list_nemUnauthorized_khiKhongResolveDuocUser() {
        var repository = mock(SearchHistoryRepository.class);
        var users = mock(UserRepository.class);
        when(users.findFirstByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        var service = new SearchHistoryService(repository, users);

        assertThrows(ApiException.class, () -> service.list(authOf(EMAIL), 10));
    }

    @Test
    void delete_uyQuyenChoRepository() {
        var repository = mock(SearchHistoryRepository.class);
        var users = mock(UserRepository.class);
        var user = new UserEntity(EMAIL, "hash", UserRole.USER);
        when(users.findFirstByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        var service = new SearchHistoryService(repository, users);
        UUID historyId = UUID.randomUUID();

        service.delete(authOf(EMAIL), historyId);

        verify(repository).delete(user.getId(), historyId);
    }

    private static Authentication authOf(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }
}
