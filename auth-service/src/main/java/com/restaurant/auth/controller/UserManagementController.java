package com.restaurant.auth.controller;

import com.restaurant.auth.dto.UpdateUserRolesRequest;
import com.restaurant.auth.dto.UserAdminResponse;
import com.restaurant.auth.service.UserManagementService;
import com.restaurant.shared.dto.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAnyRole('ADMIN')")
@RequiredArgsConstructor
@Validated
public class UserManagementController {

    private final UserManagementService userManagementService;

    /**
     * GET /api/admin/users?page=0&size=20&search=john&role=ROLE_GUEST
     */
    @GetMapping
    public ResponseEntity<PageResponse<UserAdminResponse>> getUsers(
            @RequestParam(defaultValue = "") String search,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageResponse<UserAdminResponse> result = userManagementService.getUsers(search, role, page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * PATCH /api/admin/users/{id}/roles
     * Body: { "roles": ["ROLE_WAITER"] }
     */
    @PatchMapping("/{id}/roles")
    public ResponseEntity<UserAdminResponse> updateRoles(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRolesRequest request,
            Principal principal
    ) {
        UserAdminResponse updated = userManagementService.updateRoles(id, request.roles(), principal.getName());
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/admin/users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            Principal principal
    ) {
        userManagementService.deleteUser(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
