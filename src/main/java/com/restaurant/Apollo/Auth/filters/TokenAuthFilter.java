package com.restaurant.Apollo.Auth.filters;

import com.restaurant.Apollo.Auth.service.TokenService;
import com.restaurant.Apollo.UserManagement.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

public class TokenAuthFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    public TokenAuthFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        try {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                String raw = auth.substring(7);
                User user = tokenService.validate(raw);
                if (user != null) {
                    Set<SimpleGrantedAuthority> authorities = user.getRoles() != null ?
                            user.getRoles().stream()
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toSet()) :
                            Collections.emptySet();

                    var authentication = new UsernamePasswordAuthenticationToken(
                            user.getEmail(),    // principal
                            null,               // credentials (null pentru că nu avem nevoie de ele după autentificare)
                            authorities         // authorities
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            logger.error("Could not set user authentication", ex);
        }

        chain.doFilter(request, response);
    }
}
