package com.restaurant.Apollo.Auth.model;

import com.restaurant.Apollo.UserManagement.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity @Table(name = "auth_tokens", indexes = {
        @Index(name = "idx_authtoken_token_hash", columnList = "tokenHash", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64) // SHA-256 hex
    private String tokenHash;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private User user;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;
}
