package com.restaurant.gateway.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.WebFilter;

@Configuration
public class InternalPathBlockFilter {

    @Bean
    public WebFilter blockInternalPaths() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            if (path.startsWith("/api/auth/internal/")) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };
    }
}
