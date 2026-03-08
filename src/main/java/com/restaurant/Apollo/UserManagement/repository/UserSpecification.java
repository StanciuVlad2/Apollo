package com.restaurant.Apollo.UserManagement.repository;

import com.restaurant.Apollo.UserManagement.model.User;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecification {

    private UserSpecification() {}

    /**
     * Case-insensitive partial match on the email column.
     */
    public static Specification<User> emailContains(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("email")), "%" + search.trim().toLowerCase() + "%");
        };
    }

    /**
     * Exact match against a role value in the @ElementCollection.
     * Uses an INNER JOIN on the roles collection.
     */
    public static Specification<User> hasRole(String role) {
        return (root, query, cb) -> {
            if (role == null || role.isBlank()) return cb.conjunction();
            // Deduplicate rows that could arise from the join
            if (query != null) query.distinct(true);
            var rolesJoin = root.join("roles", JoinType.INNER);
            return cb.equal(rolesJoin, role);
        };
    }

    /**
     * Combines email search and role filter with AND logic.
     */
    public static Specification<User> build(String search, String role) {
        return Specification
                .where(emailContains(search))
                .and(hasRole(role));
    }
}
