package com.familytree.controller;

import com.familytree.dto.admin.AdminLinkUserRequest;
import com.familytree.dto.admin.CreateUserRequest;
import com.familytree.dto.admin.UpdateUserRequest;
import com.familytree.dto.admin.UserWithProfileResponse;
import com.familytree.dto.response.UserResponse;
import com.familytree.dto.user.UserTreeProfileResponse;
import com.familytree.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for admin operations
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AdminService adminService;

    // ==================== User Management ====================

    /**
     * Get all users with pagination
     * GET /api/admin/users
     */
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Admin '{}' fetching all users", authentication.getName());
        Page<UserResponse> users = adminService.getAllUsers(authentication.getName(), pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * Get all users with their profile mappings for a specific tree
     * GET /api/admin/trees/{treeId}/users
     */
    @GetMapping("/trees/{treeId}/users")
    public ResponseEntity<List<UserWithProfileResponse>> getUsersWithProfiles(
            @PathVariable UUID treeId,
            Authentication authentication) {

        log.info("Admin '{}' fetching users with profiles for tree '{}'", authentication.getName(), treeId);
        List<UserWithProfileResponse> users = adminService.getUsersWithProfiles(treeId, authentication.getName());
        return ResponseEntity.ok(users);
    }

    /**
     * Create a new user
     * POST /api/admin/users
     */
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request,
            Authentication authentication) {

        log.info("Admin '{}' creating new user", authentication.getName());
        UserResponse user = adminService.createUser(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Update a user
     * PUT /api/admin/users/{userId}
     */
    @PutMapping("/users/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {

        log.info("Admin '{}' updating user '{}'", authentication.getName(), userId);
        UserResponse user = adminService.updateUser(userId, request, authentication.getName());
        return ResponseEntity.ok(user);
    }

    /**
     * Delete a user
     * DELETE /api/admin/users/{userId}
     */
    @DeleteMapping("/users/{userId}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID userId,
            Authentication authentication) {

        log.info("Admin '{}' deleting user '{}'", authentication.getName(), userId);
        adminService.deleteUser(userId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // ==================== User-Individual Mapping ====================

    /**
     * Link a user to an individual in a tree
     * POST /api/admin/trees/{treeId}/user-profiles
     */
    @PostMapping("/trees/{treeId}/user-profiles")
    public ResponseEntity<UserTreeProfileResponse> linkUserToIndividual(
            @PathVariable UUID treeId,
            @Valid @RequestBody AdminLinkUserRequest request,
            Authentication authentication) {

        log.info("Admin '{}' linking user '{}' to individual '{}' in tree '{}'",
                authentication.getName(), request.getUserId(), request.getIndividualId(), treeId);

        UserTreeProfileResponse response = adminService.linkUserToIndividual(
                treeId, request.getUserId(), request.getIndividualId(), authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Unlink a user from an individual in a tree
     * DELETE /api/admin/trees/{treeId}/user-profiles/{userId}
     */
    @DeleteMapping("/trees/{treeId}/user-profiles/{userId}")
    public ResponseEntity<Void> unlinkUserFromIndividual(
            @PathVariable UUID treeId,
            @PathVariable UUID userId,
            Authentication authentication) {

        log.info("Admin '{}' unlinking user '{}' from tree '{}'",
                authentication.getName(), userId, treeId);

        adminService.unlinkUserFromIndividual(treeId, userId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // ==================== Create Users from Tree ====================

    /**
     * Create a user from a specific individual in a tree
     * POST /api/admin/trees/{treeId}/create-user-from-individual/{individualId}
     */
    @PostMapping("/trees/{treeId}/create-user-from-individual/{individualId}")
    public ResponseEntity<UserResponse> createUserFromIndividual(
            @PathVariable UUID treeId,
            @PathVariable UUID individualId,
            Authentication authentication) {

        log.info("Admin '{}' creating user from individual '{}' in tree '{}'",
                authentication.getName(), individualId, treeId);

        UserResponse user = adminService.createUserFromIndividual(treeId, individualId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Bulk create users from all unlinked individuals in a tree
     * POST /api/admin/trees/{treeId}/create-users-from-tree
     */
    @PostMapping("/trees/{treeId}/create-users-from-tree")
    public ResponseEntity<List<UserResponse>> createUsersFromTree(
            @PathVariable UUID treeId,
            Authentication authentication) {

        log.info("Admin '{}' bulk creating users from tree '{}'", authentication.getName(), treeId);

        List<UserResponse> users = adminService.createUsersFromTree(treeId, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(users);
    }
}
