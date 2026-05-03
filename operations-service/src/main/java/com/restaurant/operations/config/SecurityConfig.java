package com.restaurant.operations.config;

import com.restaurant.shared.config.BaseSecurityConfig;
import com.restaurant.shared.security.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig extends BaseSecurityConfig {

    public SecurityConfig(TokenService tokenService) {
        super(tokenService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        super.configure(http);
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/menu-items", "/api/menu-items/*").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/stock/events").permitAll()
                .requestMatchers(HttpMethod.GET, "/uploads/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/stock/import").hasAnyRole("MANAGER", "ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
        );
        return http.build();
    }
}
