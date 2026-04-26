package com.restaurant.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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

    public GeneratedToken issue(Long userId, String email, Set<String> roles) {
        long now = System.currentTimeMillis();
        long expiry = now + ttlMinutes * 60_000;
        String jwt = Jwts.builder()
                .subject(email)
                .issuer(issuer)
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(expiry))
                .signWith(signingKey)
                .compact();
        return new GeneratedToken(jwt, ttlMinutes * 60);
    }

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

    public UserPrincipal toPrincipal(Claims claims) {
        String email = claims.getSubject();
        Long userId = claims.get("userId", Long.class);
        Object rolesRaw = claims.get("roles");
        Set<String> roles = new HashSet<>();
        if (rolesRaw instanceof java.util.Collection<?> col) {
            col.forEach(r -> roles.add(r.toString()));
        }
        return new UserPrincipal(userId, email, roles);
    }
}
