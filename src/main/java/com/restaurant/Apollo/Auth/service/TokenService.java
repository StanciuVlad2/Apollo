package com.restaurant.Apollo.Auth.service;

import com.restaurant.Apollo.UserManagement.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

@Service
public class TokenService {

    private final SecretKey signingKey;
    private final long ttlMinutes;
    private final String issuer;

    public TokenService(@Value("${app.token.secret}") String secret,
                        @Value("${app.token.ttl-minutes}") long ttlMinutes,
                        @Value("${app.token.issuer:restaurant-app}") String issuer) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMinutes = ttlMinutes;
        this.issuer = issuer;
    }

    public record GeneratedToken(String rawToken, long expiresInSeconds) {}

    public GeneratedToken issue(User user) {
        long nowMillis = System.currentTimeMillis();
        long expiryMillis = nowMillis + (ttlMinutes * 60 * 1000);

        String jwt = Jwts.builder()
                .subject(user.getEmail())
                .issuer(issuer)
                .claim("roles", user.getRoles())
                .claim("userId", user.getId())
                .issuedAt(new Date(nowMillis))
                .expiration(new Date(expiryMillis))
                .signWith(signingKey)
                .compact();

        return new GeneratedToken(jwt, ttlMinutes * 60);
    }

    /**
     * Validates the JWT and returns the claims if valid, null otherwise.
     */
    public Claims validate(String token) {
        if (token == null || token.isBlank()) return null;
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Extracts the email (subject) from a valid JWT.
     */
    public String getEmail(Claims claims) {
        return claims.getSubject();
    }

    /**
     * Extracts roles from JWT claims.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getRoles(Claims claims) {
        var roles = claims.get("roles");
        if (roles instanceof java.util.Collection<?> col) {
            return new java.util.HashSet<>(col.stream().map(Object::toString).toList());
        }
        return Set.of();
    }
}
