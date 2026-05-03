package com.restaurant.gateway.filter;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthGatewayFilter implements GlobalFilter, Ordered {

    private final SecretKey signingKey;
    private final String issuer;

    private static final List<String> PUBLIC_POST = List.of(
            "/api/auth/login", "/api/auth/register", "/api/cocktails/generate"
    );
    private static final List<String> PUBLIC_GET_PREFIXES = List.of(
            "/api/auth/verify-email", "/api/menu-items", "/api/tables", "/api/settings", "/api/stock/events"
    );

    public AuthGatewayFilter(
            @Value("${app.token.secret}") String secret,
            @Value("${app.token.issuer:restaurant-app}") String issuer) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        if (isPublic(path, method)) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Jwts.parser().verifyWith(signingKey).requireIssuer(issuer)
                    .build().parseSignedClaims(authHeader.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        return chain.filter(exchange);
    }

    private boolean isPublic(String path, HttpMethod method) {
        if (method == HttpMethod.POST && PUBLIC_POST.stream().anyMatch(path::equals)) return true;
        if (method == HttpMethod.GET && PUBLIC_GET_PREFIXES.stream().anyMatch(path::startsWith)) return true;
        return false;
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
