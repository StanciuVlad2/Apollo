package com.restaurant.Apollo.Auth.service;

import com.restaurant.Apollo.Auth.model.AuthToken;
import com.restaurant.Apollo.Auth.repository.AuthTokenRepository;
import com.restaurant.Apollo.UserManagement.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;

@Service
public class TokenService {

    private static final SecureRandom RNG = new SecureRandom();
    @Autowired
    private final AuthTokenRepository tokens;
    private final long ttlMinutes;
    private final boolean rotateOnLogin;

    public TokenService(AuthTokenRepository tokens,
                        @Value("${app.token.ttl-minutes}") long ttlMinutes,
                        @Value("${app.token.rotate-on-login:false}") boolean rotateOnLogin) {
        this.tokens = tokens;
        this.ttlMinutes = ttlMinutes;
        this.rotateOnLogin = rotateOnLogin;
    }

    public record GeneratedToken(String rawToken, long expiresInSeconds) {}

    public GeneratedToken issue(User user) {
        if (rotateOnLogin) revokeAllFor(user);

        String raw = randomToken();
        String hash = sha256(raw);
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlMinutes * 60);

        AuthToken at = AuthToken.builder()
                .tokenHash(hash)
                .user(user)
                .createdAt(now)
                .expiresAt(exp)
                .revoked(false)
                .build();
        tokens.save(at);

        return new GeneratedToken(raw, ttlMinutes * 60);
    }

    @Transactional(readOnly = true)
    public User validate(String rawToken) {
        if (rawToken == null) return null;
        String hash = sha256(rawToken);
        return tokens.findByTokenHashAndRevokedFalseAndExpiresAtAfterWithUser(hash, Instant.now())
                .map(AuthToken::getUser)
                .orElse(null);
    }

    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return;
        tokens.findByTokenHashAndRevokedFalse(sha256(rawToken)).ifPresent(t -> {
            t.setRevoked(true);
            tokens.save(t);
        });
    }

    /** 🔧 Metoda care lipsea */
    @Transactional
    public int revokeAllFor(User user) {
        if (user == null || user.getId() == null) return 0;
        return tokens.revokeAllActiveByUserId(user.getId());
    }

    public long purgeExpired() {
        return tokens.deleteByExpiresAtBefore(Instant.now());
    }

    private static String randomToken() {
        byte[] buf = new byte[32]; // 256-bit
        RNG.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    private static String sha256(String raw) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            var out = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
