package com.restaurant.Apollo.Auth.repository;

import com.restaurant.Apollo.Auth.model.EmailVerificationToken;
import com.restaurant.Apollo.UserManagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByToken(String token);
    void deleteByUser(User user);
}
