package com.restaurant.Apollo.Auth.repository;

import com.restaurant.Apollo.Auth.model.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByTokenHashAndRevokedFalse(String tokenHash);

    Optional<AuthToken> findByTokenHashAndRevokedFalseAndExpiresAtAfter(String tokenHash, Instant time);

    @Query("SELECT t FROM AuthToken t JOIN FETCH t.user u LEFT JOIN FETCH u.roles " +
            "WHERE t.tokenHash = :hash AND t.revoked = false AND t.expiresAt > :time")
    Optional<AuthToken> findByTokenHashAndRevokedFalseAndExpiresAtAfterWithUser(
            @Param("hash") String hash,
            @Param("time") Instant time);

    long deleteByExpiresAtBefore(Instant time);

    @Modifying
    @Query("update AuthToken t set t.revoked = true " +
            "where t.user.id = :userId and t.revoked = false")
    int revokeAllActiveByUserId(@Param("userId") Long userId);
}
