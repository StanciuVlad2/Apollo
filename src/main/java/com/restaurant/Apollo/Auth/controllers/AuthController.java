package com.restaurant.Apollo.Auth.controllers;

import com.restaurant.Apollo.Auth.dto.AuthResponse;
import com.restaurant.Apollo.Auth.dto.MeResponse;
import com.restaurant.Apollo.Auth.model.EmailVerificationToken;
import com.restaurant.Apollo.Auth.repository.EmailVerificationTokenRepository;
import com.restaurant.Apollo.Auth.service.EmailService;
import com.restaurant.Apollo.Auth.service.TokenService;
import com.restaurant.Apollo.UserManagement.dto.LoginRequest;
import com.restaurant.Apollo.UserManagement.dto.RegisterRequest;
import com.restaurant.Apollo.UserManagement.enums.UserRoles;
import com.restaurant.Apollo.UserManagement.model.User;
import com.restaurant.Apollo.UserManagement.repository.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final EmailVerificationTokenRepository verificationTokenRepository;

    public AuthController(UserRepository users, PasswordEncoder encoder, TokenService tokenService,
                         EmailService emailService, EmailVerificationTokenRepository verificationTokenRepository) {
        this.users = users;
        this.encoder = encoder;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.verificationTokenRepository = verificationTokenRepository;
    }

    @PostMapping("/register")
    @Transactional
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
                .roles(Set.of(UserRoles.ROLE_GUEST.toString()))
                .emailVerified(req.skipEmailVerification())
                .build();
        users.save(u);

        if (req.skipEmailVerification()) {
            return ResponseEntity.ok().body("User created successfully.");
        }

        // Generate verification token
        String token = UUID.randomUUID().toString();
        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
                .token(token)
                .user(u)
                .expiryDate(LocalDateTime.now().plusHours(24))
                .used(false)
                .build();
        verificationTokenRepository.save(verificationToken);

        try {
            emailService.sendVerificationEmail(email, token);
            log.info("Verification email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email", e);
        }

        return ResponseEntity.ok().body("Registration successful. Please check your email to verify your account.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        var user = users.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!encoder.matches(req.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            return ResponseEntity.status(403).body("Please verify your email before logging in. Check your inbox.");
        }

        var issued = tokenService.issue(user);
        return ResponseEntity.ok(new AuthResponse(issued.rawToken(), issued.expiresInSeconds()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // JWT is stateless — the client simply discards the token.
        // This endpoint exists for API compatibility.
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(new MeResponse(false, null, Set.of()));
        }
        var user = users.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(new MeResponse(true, user.getEmail(), user.getRoles()));
    }

    @GetMapping("/verify-email")
    @Transactional
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        var verificationToken = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid verification token"));

        if (verificationToken.isUsed()) {
            return ResponseEntity.badRequest().body("Token already used");
        }

        if (verificationToken.isExpired()) {
            return ResponseEntity.badRequest().body("Token expired");
        }

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        users.save(user);

        verificationToken.setUsed(true);
        verificationTokenRepository.save(verificationToken);

        log.info("Email verified for user: {}", user.getEmail());
        return ResponseEntity.ok().body("Email verified successfully! You can now login.");
    }
}
