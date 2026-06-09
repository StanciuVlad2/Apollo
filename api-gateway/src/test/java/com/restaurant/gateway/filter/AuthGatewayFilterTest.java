package com.restaurant.gateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Teste unitare pentru AuthGatewayFilter.
 *
 * Acoperire:
 *  - #6  GET /api/reservations/availability → public (fără token)
 *  - #6  POST /api/reservations → public (fără token)
 *  - M4  POST /api/cocktails/generate → protejat (fără token → 401)
 *  - M4  POST /api/cocktails/generate cu JWT valid → trece
 *  - General: endpoint protejat fără token → 401
 *  - General: endpoint protejat cu JWT valid → trece
 */
class AuthGatewayFilterTest {

    private static final String SECRET =
            "test-secret-key-min-256-bits-long-enough-for-hmac-sha256";
    private static final String ISSUER = "restaurant-app";

    private AuthGatewayFilter filter;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new AuthGatewayFilter(SECRET, ISSUER);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    // ------------------------------------------------------------------
    // #6 — GET /api/reservations/availability este public
    // ------------------------------------------------------------------

    @Test
    void getReservationsAvailability_noToken_isPublic() {
        MockServerWebExchange exchange = exchangeFor(
                HttpMethod.GET, "/api/reservations/availability");

        filter.filter(exchange, chain).block();

        // Chain trebuie apelat — requestul nu a fost blocat
        verify(chain).filter(any());
        assertEquals(200, exchange.getResponse().getStatusCode() == null ? 200
                : exchange.getResponse().getStatusCode().value());
    }

    @Test
    void getReservationsAvailabilityWithParams_noToken_isPublic() {
        MockServerWebExchange exchange = exchangeFor(
                HttpMethod.GET, "/api/reservations/availability");

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }

    // ------------------------------------------------------------------
    // #6 — POST /api/reservations este public
    // ------------------------------------------------------------------

    @Test
    void postReservations_noToken_isPublic() {
        MockServerWebExchange exchange = exchangeFor(
                HttpMethod.POST, "/api/reservations");

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }

    // ------------------------------------------------------------------
    // M4 — POST /api/cocktails/generate este PROTEJAT (nu mai e public)
    // ------------------------------------------------------------------

    @Test
    void postCocktailsGenerate_noToken_returns401() {
        MockServerWebExchange exchange = exchangeFor(
                HttpMethod.POST, "/api/cocktails/generate");

        filter.filter(exchange, chain).block();

        // Chain NU trebuie apelat
        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void postCocktailsGenerate_withValidToken_passes() {
        String token = generateValidToken();
        MockServerWebExchange exchange = exchangeForWithAuth(
                HttpMethod.POST, "/api/cocktails/generate", token);

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }

    // ------------------------------------------------------------------
    // General — endpoint protejat fără token → 401
    // ------------------------------------------------------------------

    @Test
    void protectedEndpoint_noToken_returns401() {
        MockServerWebExchange exchange = exchangeFor(
                HttpMethod.GET, "/api/orders");

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void protectedEndpoint_withValidToken_passes() {
        String token = generateValidToken();
        MockServerWebExchange exchange = exchangeForWithAuth(
                HttpMethod.GET, "/api/orders", token);

        filter.filter(exchange, chain).block();

        verify(chain).filter(any());
    }

    @Test
    void protectedEndpoint_withExpiredToken_returns401() {
        String expired = generateExpiredToken();
        MockServerWebExchange exchange = exchangeForWithAuth(
                HttpMethod.GET, "/api/orders", expired);

        filter.filter(exchange, chain).block();

        verify(chain, never()).filter(any());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private MockServerWebExchange exchangeFor(HttpMethod method, String path) {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(method, path)
                .build();
        return MockServerWebExchange.from(request);
    }

    private MockServerWebExchange exchangeForWithAuth(HttpMethod method, String path, String token) {
        MockServerHttpRequest request = MockServerHttpRequest
                .method(method, path)
                .header("Authorization", "Bearer " + token)
                .build();
        return MockServerWebExchange.from(request);
    }

    private String generateValidToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("user@test.com")
                .issuer(ISSUER)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();
    }

    private String generateExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject("user@test.com")
                .issuer(ISSUER)
                .expiration(new Date(System.currentTimeMillis() - 60_000))
                .signWith(key)
                .compact();
    }
}
