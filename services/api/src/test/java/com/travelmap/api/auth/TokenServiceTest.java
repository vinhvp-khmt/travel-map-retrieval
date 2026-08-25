package com.travelmap.api.auth;

import tools.jackson.databind.ObjectMapper;
import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.auth.repository.RefreshTokenRepository;
import com.travelmap.api.auth.service.TokenService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenServiceTest {

    @Test
    void issuedAccessTokenCanBeVerifiedAndRefreshTokenIsNotStoredRaw() {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        TokenService service = new TokenService(repository, new ObjectMapper(),
                "unit-test-secret-with-more-than-thirty-two-characters", 30, 7);
        UserEntity user = new UserEntity("user@example.com", "hash", UserRole.USER);

        var pair = service.issuePair(user);
        var claims = service.parseAccessToken(pair.accessToken());

        assertEquals(user.getId(), claims.userId());
        assertEquals(UserRole.USER, claims.role());
        assertFalse(pair.refreshToken().isBlank());
        verify(repository).save(any());
    }
}
