package com.travelmap.api.auth.service;

import com.travelmap.api.auth.dto.AuthResponse;
import com.travelmap.api.auth.dto.LoginRequest;
import com.travelmap.api.auth.dto.RegisterRequest;
import com.travelmap.api.auth.dto.UserResponse;
import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.auth.model.UserStatus;
import com.travelmap.api.auth.repository.UserRepository;
import com.travelmap.api.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;

@Service
public class AuthenticationService {
    private static final String DUMMY_HASH = "$2a$10$7EqJtq98hPqEX7fNZaFWoO5uQVkJz1q2y5QZ8Q0bXgMWW0tQ0xE1G";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    public AuthenticationService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.clock = Clock.systemUTC();
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "Email is already registered");
        }
        UserEntity user = new UserEntity(email, passwordEncoder.encode(request.password()), UserRole.USER);
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse login(LoginRequest request) {
        Instant now = clock.instant();
        String email = normalizeEmail(request.email());
        UserEntity user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw invalidCredentials();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Account is disabled");
        }
        if (user.isLocked(now)) {
            throw new ApiException(HttpStatus.LOCKED, "ACCOUNT_LOCKED", "Account is temporarily locked");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin(now);
            throw invalidCredentials();
        }
        user.recordSuccessfulLogin(now);
        return tokenService.issuePair(user);
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect");
    }
}
