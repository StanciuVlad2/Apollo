package com.restaurant.reports.config;

import com.restaurant.shared.config.BaseSecurityConfig;
import com.restaurant.shared.security.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig extends BaseSecurityConfig {

    public SecurityConfig(TokenService tokenService) {
        super(tokenService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        super.configure(http);
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/reports/**").hasAnyRole("MANAGER", "ADMIN")
                .anyRequest().authenticated()
        );
        return http.build();
    }
}
