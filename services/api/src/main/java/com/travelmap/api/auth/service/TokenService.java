package com.travelmap.api.auth.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.travelmap.api.auth.dto.AuthResponse;
import com.travelmap.api.auth.model.RefreshTokenEntity;
import com.travelmap.api.auth.model.UserEntity;
import com.travelmap.api.auth.model.UserRole;
import com.travelmap.api.auth.model.UserStatus;
import com.travelmap.api.auth.repository.RefreshTokenRepository;
import com.travelmap.api.common.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TokenService {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final byte[] signingSecret;
    private final Duration accessLifetime;
    private final Duration refreshLifetime;

    public TokenService(
            RefreshTokenRepository refreshTokenRepository,
            ObjectMapper objectMapper,
            @Value("${travelmap.security.jwt-secret}") String signingSecret,
            @Value("${travelmap.security.access-token-minutes:30}") long accessMinutes,
            @Value("${travelmap.security.refresh-token-days:7}") long refreshDays
    ) {
        if (signingSecret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 characters");
        }
        this.refreshTokenRepository = refreshTokenRepository;
        this.objectMapper = objectMapper;
        this.clock = Clock.systemUTC();
        this.signingSecret = signingSecret.getBytes(StandardCharsets.UTF_8);
        this.accessLifetime = Duration.ofMinutes(accessMinutes);
        this.refreshLifetime = Duration.ofDays(refreshDays);
    }

    @Transactional
    public AuthResponse issuePair(UserEntity user) {
        Instant now = clock.instant();
        String refreshToken = randomToken();
        refreshTokenRepository.save(new RefreshTokenEntity(
                user, sha256(refreshToken), now.plus(refreshLifetime), now));
        return new AuthResponse("Bearer", createAccessToken(user, now), accessLifetime.toSeconds(),
                refreshToken, refreshLifetime.toSeconds());
    }

    @Transactional
    public AuthResponse rotate(String rawRefreshToken) {
        Instant now = clock.instant();
        RefreshTokenEntity stored = refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
                .orElseThrow(TokenService::invalidRefreshToken);
        if (stored.getRevokedAt() != null || !stored.getExpiresAt().isAfter(now)
                || stored.getUser().getStatus() != UserStatus.ACTIVE) {
            throw invalidRefreshToken();
        }
        stored.revoke(now);
        return issuePair(stored.getUser());
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(sha256(rawRefreshToken))
                .ifPresent(token -> token.revoke(clock.instant()));
    }

    public JwtClaims parseAccessToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) throw unauthorized();
            byte[] expected = sign(parts[0] + "." + parts[1]);
            byte[] actual = URL_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) throw unauthorized();
            Map<String, Object> payload = objectMapper.readValue(
                    URL_DECODER.decode(parts[1]), new TypeReference<>() {});
            long expiresAt = ((Number) payload.get("exp")).longValue();
            if (expiresAt <= clock.instant().getEpochSecond()) throw unauthorized();
            return new JwtClaims(
                    java.util.UUID.fromString((String) payload.get("sub")),
                    (String) payload.get("email"),
                    UserRole.valueOf((String) payload.get("role")),
                    expiresAt);
        } catch (ApiException exception) {
            throw exception;
        } catch (Exception exception) {
            throw unauthorized();
        }
    }

    private String createAccessToken(UserEntity user, Instant now) {
        try {
            String header = URL_ENCODER.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}"
                    .getBytes(StandardCharsets.UTF_8));
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("sub", user.getId().toString());
            claims.put("email", user.getEmail());
            claims.put("role", user.getRole().name());
            claims.put("iat", now.getEpochSecond());
            claims.put("exp", now.plus(accessLifetime).getEpochSecond());
            String payload = URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
            String unsigned = header + "." + payload;
            return unsigned + "." + URL_ENCODER.encodeToString(sign(unsigned));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not create access token", exception);
        }
    }

    private byte[] sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(signingSecret, "HmacSHA256"));
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String randomToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid or expired");
    }

    private static ApiException unauthorized() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_ACCESS_TOKEN", "Access token is invalid or expired");
    }
}
