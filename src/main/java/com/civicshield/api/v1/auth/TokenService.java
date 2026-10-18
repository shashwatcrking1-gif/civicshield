package com.civicshield.api.v1.auth;

import com.civicshield.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages opaque 256-bit Bearer tokens stored as SHA-256 hashes in memory,
 * and tracks login attempts to enforce rate limiting.
 */
@Service
public class TokenService {

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, TokenInfo> tokens = new ConcurrentHashMap<>();
    private final Map<String, List<LocalDateTime>> failedAttempts = new ConcurrentHashMap<>();

    @Value("${civicshield.api.token-ttl-minutes:480}")
    private long tokenTtlMinutes;

    public record TokenInfo(Long userId, String email, String name, String role, LocalDateTime expiresAt) {
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    public record TokenResult(String rawToken, LocalDateTime expiresAt) {}

    public TokenResult createToken(User user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String rawToken = HexFormat.of().formatHex(bytes);
        String hash = sha256(rawToken);

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(tokenTtlMinutes);
        tokens.put(hash, new TokenInfo(user.getUserId(), user.getEmail(), user.getName(), user.getRole().name(), expiresAt));
        return new TokenResult(rawToken, expiresAt);
    }

    public TokenInfo validateToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;
        String hash = sha256(rawToken.trim());
        TokenInfo info = tokens.get(hash);
        if (info == null) return null;
        if (info.isExpired()) {
            tokens.remove(hash);
            return null;
        }
        return info;
    }

    public void revokeToken(String rawToken) {
        if (rawToken != null) {
            tokens.remove(sha256(rawToken.trim()));
        }
    }

    public boolean isRateLimited(String email) {
        if (email == null) return false;
        String key = email.trim().toLowerCase(Locale.ROOT);
        List<LocalDateTime> attempts = failedAttempts.get(key);
        if (attempts == null) return false;

        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(5);
        synchronized (attempts) {
            attempts.removeIf(t -> t.isBefore(cutoff));
            return attempts.size() >= 5;
        }
    }

    public void recordFailedLogin(String email) {
        if (email == null) return;
        String key = email.trim().toLowerCase(Locale.ROOT);
        failedAttempts.compute(key, (k, list) -> {
            if (list == null) list = Collections.synchronizedList(new ArrayList<>());
            list.add(LocalDateTime.now());
            return list;
        });
    }

    public void resetFailedAttempts(String email) {
        if (email != null) {
            failedAttempts.remove(email.trim().toLowerCase(Locale.ROOT));
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }
}
