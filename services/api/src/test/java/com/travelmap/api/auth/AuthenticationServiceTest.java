package com.travelmap.api.auth;

import com.travelmap.api.auth.dto.AuthResponse;
import com.travelmap.api.auth.dto.LoginRequest;
import com.travelmap.api.auth.dto.RegisterRequest;
import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.auth.service.AuthenticationService;
import com.travelmap.api.auth.service.TokenService;
import com.travelmap.api.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock TokenService tokenService;

    @Test
    void registerNormalizesEmailAndCreatesUserRole() {
        when(userRepository.existsByEmailIgnoreCase("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("hash");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AuthenticationService service = new AuthenticationService(userRepository, passwordEncoder, tokenService);

        var result = service.register(new RegisterRequest(" User@Example.com ", "Password1"));

        assertEquals("user@example.com", result.email());
        assertEquals("USER", result.role());
    }

    @Test
    void fiveFailuresInsideWindowLockAccountForFifteenMinutes() {
        UserEntity user = new UserEntity("user@example.com", "hash", UserRole.USER);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        AuthenticationService service = new AuthenticationService(userRepository, passwordEncoder, tokenService);

        for (int attempt = 0; attempt < 5; attempt++) {
            ApiException exception = assertThrows(ApiException.class,
                    () -> service.login(new LoginRequest("user@example.com", "wrong")));
            assertEquals("INVALID_CREDENTIALS", exception.getCode());
        }

        assertTrue(user.isLocked(Instant.now()));
        assertTrue(user.getLockedUntil().isAfter(Instant.now().plusSeconds(14 * 60)));
    }

    @Test
    void validCredentialsReturnTokenPair() {
        UserEntity user = new UserEntity("user@example.com", "hash", UserRole.USER);
        AuthResponse pair = new AuthResponse("Bearer", "access", 1800, "refresh", 604800);
        when(userRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password1", "hash")).thenReturn(true);
        when(tokenService.issuePair(user)).thenReturn(pair);
        AuthenticationService service = new AuthenticationService(userRepository, passwordEncoder, tokenService);

        assertEquals(pair, service.login(new LoginRequest("user@example.com", "Password1")));
        verify(tokenService).issuePair(user);
    }
}
