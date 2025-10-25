package com.restaurant.Apollo.Auth.controllers;

import com.restaurant.Apollo.Auth.dto.AuthResponse;
import com.restaurant.Apollo.Auth.dto.MeResponse;
import com.restaurant.Apollo.Auth.service.TokenService;
import com.restaurant.Apollo.UserManagement.dto.LoginRequest;
import com.restaurant.Apollo.UserManagement.dto.RegisterRequest;
import com.restaurant.Apollo.UserManagement.model.User;
import com.restaurant.Apollo.UserManagement.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokenService;

    public AuthController(UserRepository users, PasswordEncoder encoder, TokenService tokenService) {
        this.users = users;
        this.encoder = encoder;
        this.tokenService = tokenService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("Email already in use");
        }
        if (req.password() == null || req.password().length() < 6) {
            return ResponseEntity.badRequest().body("Password too short");
        }
        User u = User.builder()
                .email(email)
                .password(encoder.encode(req.password()))
                .roles(Set.of("ROLE_USER"))
                .build();
        users.save(u);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        var user = users.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!encoder.matches(req.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        var issued = tokenService.issue(user);
        return ResponseEntity.ok(new AuthResponse(issued.rawToken(), issued.expiresInSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(name = "Authorization", required = false) String auth) {
        if (auth != null && auth.startsWith("Bearer ")) {
            tokenService.revoke(auth.substring(7));
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(new MeResponse(false, null, java.util.Set.of()));
        }
        var user = users.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(new MeResponse(true, user.getEmail(), user.getRoles()));
    }
}
