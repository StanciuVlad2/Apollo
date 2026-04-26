package com.restaurant.shared.config;

import com.restaurant.shared.exception.GlobalExceptionHandler;
import com.restaurant.shared.feign.JwtFeignInterceptor;
import com.restaurant.shared.security.TokenService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class SharedLibAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TokenService tokenService(
            @org.springframework.beans.factory.annotation.Value("${app.token.secret}") String secret,
            @org.springframework.beans.factory.annotation.Value("${app.token.ttl-minutes}") long ttlMinutes,
            @org.springframework.beans.factory.annotation.Value("${app.token.issuer:restaurant-app}") String issuer) {
        return new TokenService(secret, ttlMinutes, issuer);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtFeignInterceptor jwtFeignInterceptor() {
        return new JwtFeignInterceptor();
    }
}
