package com.restaurant.Apollo.UserManagement.config;

import com.restaurant.Apollo.UserManagement.enums.UserRoles;
import com.restaurant.Apollo.UserManagement.model.User;
import com.restaurant.Apollo.UserManagement.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
public class TestUserInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public TestUserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        log.info("Checking and creating test users for each role...");

        createTestUserIfNotExists(UserRoles.ROLE_GUEST);
        createTestUserIfNotExists(UserRoles.ROLE_WAITER);
        createTestUserIfNotExists(UserRoles.ROLE_CHEF);
        createTestUserIfNotExists(UserRoles.ROLE_MANAGER);
        createTestUserIfNotExists(UserRoles.ROLE_ADMIN);

        log.info("Test users initialization completed.");
    }

    private void createTestUserIfNotExists(UserRoles role) {
        String email = "vlad." + role.name().toLowerCase().replace("role_", "") + "@gmail.com";
        String password = "salut123";

        if (userRepository.existsByEmail(email)) {
            log.info("Test user already exists: {}", email);
            return;
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .roles(Set.of(role.toString()))
                .build();

        userRepository.save(user);
        log.info("Created test user: {} with role: {}", email, role);
    }
}
