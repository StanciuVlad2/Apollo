package com.restaurant.vouchers.config;

import com.restaurant.shared.security.TokenService;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;


@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor serviceTokenFeignInterceptor(TokenService tokenService) {
        return template -> {
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes) {
                return; // HTTP thread — JwtFeignInterceptor from shared-lib handles it
            }
            // No HTTP context (e.g. Kafka consumer thread) — mint a service token
            String token = tokenService.issue(0L, "vouchers-service@internal", Set.of("ROLE_ADMIN")).rawToken();
            template.header("Authorization", "Bearer " + token);
        };
    }
}
