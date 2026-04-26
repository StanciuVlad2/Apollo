package com.restaurant.shared.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class JwtFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            String auth = sra.getRequest().getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                template.header("Authorization", auth);
            }
        }
    }
}
