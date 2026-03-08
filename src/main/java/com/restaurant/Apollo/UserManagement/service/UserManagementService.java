package com.restaurant.Apollo.UserManagement.service;

import com.restaurant.Apollo.Auth.repository.EmailVerificationTokenRepository;
import com.restaurant.Apollo.UserManagement.dto.PageResponse;
import com.restaurant.Apollo.UserManagement.dto.UserAdminResponse;
import com.restaurant.Apollo.UserManagement.enums.UserRoles;
import com.restaurant.Apollo.UserManagement.model.User;
import com.restaurant.Apollo.UserManagement.repository.UserRepository;
import com.restaurant.Apollo.UserManagement.repository.UserSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> VALID_ROLES = Arrays.stream(UserRoles.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;

    /**
     * Returns a paginated, filtered list of all users.
     *
     * @param search  partial email match (case-insensitive), nullable
     * @param role    exact role filter (e.g. "ROLE_ADMIN"), nullable
     * @param page    0-based page index
     * @param size    number of items per page (capped at MAX_PAGE_SIZE)
     */
    @Transactional(readOnly = true)
    public PageResponse<UserAdminResponse> getUsers(String search, String role, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize, Sort.by(Sort.Direction.DESC, "id"));

        Page<User> result = userRepository.findAll(
                UserSpecification.build(search, role),
                pageable
        );

        return PageResponse.from(result, UserAdminResponse::from);
    }

    /**
     * Replaces roles for the target user.
     * Guards: roles must be valid enum values; cannot remove last ROLE_ADMIN.
     */
    @Transactional
    public UserAdminResponse updateRoles(Long userId, Set<String> newRoles, String requestingUserEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        // Validate all provided roles are known
        Set<String> invalid = newRoles.stream()
                .filter(r -> !VALID_ROLES.contains(r))
                .collect(Collectors.toSet());
        if (!invalid.isEmpty()) {
            throw new IllegalArgumentException("Unknown roles: " + invalid);
        }

        // Guard: if removing ROLE_ADMIN, ensure at least one other admin exists
        boolean wasAdmin = user.getRoles().contains(UserRoles.ROLE_ADMIN.name());
        boolean willBeAdmin = newRoles.contains(UserRoles.ROLE_ADMIN.name());
        if (wasAdmin && !willBeAdmin) {
            long adminCount = userRepository.countByRolesContaining(UserRoles.ROLE_ADMIN.name());
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot remove the last admin account");
            }
        }

        user.setRoles(newRoles);
        User saved = userRepository.save(user);
        log.info("Admin {} updated roles for user {} → {}", requestingUserEmail, userId, newRoles);
        return UserAdminResponse.from(saved);
    }

    /**
     * Deletes a user by ID.
     * Guards: cannot delete own account; cannot delete last admin.
     */
    @Transactional
    public void deleteUser(Long userId, String requestingUserEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));

        if (user.getEmail().equalsIgnoreCase(requestingUserEmail)) {
            throw new IllegalArgumentException("You cannot delete your own account");
        }

        if (user.getRoles().contains(UserRoles.ROLE_ADMIN.name())) {
            long adminCount = userRepository.countByRolesContaining(UserRoles.ROLE_ADMIN.name());
            if (adminCount <= 1) {
                throw new IllegalArgumentException("Cannot delete the last admin account");
            }
        }

        emailVerificationTokenRepository.deleteByUser(user);
        userRepository.delete(user);
        log.info("Admin {} deleted user {} ({})", requestingUserEmail, userId, user.getEmail());
    }
}
