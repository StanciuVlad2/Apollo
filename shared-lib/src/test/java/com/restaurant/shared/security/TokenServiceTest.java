package com.restaurant.shared.security;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private final TokenService tokenService = new TokenService(
            "MySuperSecretJWTKeyThatIsAtLeast256BitsLong!!",
            30L,
            "restaurant-app"
    );

    @Test
    void issueAndValidate_roundtrip() {
        var token = tokenService.issue(7L, "user@test.com", Set.of("ROLE_WAITER"));
        var claims = tokenService.validate(token.rawToken());
        assertThat(claims).isNotNull();
        var principal = tokenService.toPrincipal(claims);
        assertThat(principal.userId()).isEqualTo(7L);
        assertThat(principal.email()).isEqualTo("user@test.com");
        assertThat(principal.roles()).contains("ROLE_WAITER");
    }

    @Test
    void validate_invalidToken_returnsNull() {
        assertThat(tokenService.validate("not.a.jwt")).isNull();
    }

    @Test
    void validate_nullToken_returnsNull() {
        assertThat(tokenService.validate(null)).isNull();
    }
}
