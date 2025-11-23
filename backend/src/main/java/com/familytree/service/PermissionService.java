package com.familytree.service;

import com.familytree.dto.collaboration.PermissionResponse;
import com.familytree.model.FamilyTree;
import com.familytree.model.PermissionRole;
import com.familytree.model.TreePermission;
import com.familytree.model.User;
import com.familytree.repository.FamilyTreeRepository;
import com.familytree.repository.TreePermissionRepository;
import com.familytree.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing tree permissions and authorization checks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final FamilyTreeRepository treeRepository;
    private final TreePermissionRepository permissionRepository;
    private final UserRepository userRepository;

    /**
     * Check if user has view permission (viewer, editor, or owner)
     */
    public boolean hasViewPermission(UUID treeId, String userEmail) {
        FamilyTree tree = treeRepository.findById(treeId).orElse(null);
        if (tree == null) {
            return false;
        }

        // Owner has all permissions
        if (tree.getOwner().getEmail().equals(userEmail)) {
            return true;
        }

        // Check if user has any permission
        return tree.getPermissions().stream()
                .anyMatch(p -> p.getUser().getEmail().equals(userEmail));
    }

    /**
     * Check if user has edit permission (editor or owner)
     */
    public boolean hasEditPermission(UUID treeId, String userEmail) {
        FamilyTree tree = treeRepository.findById(treeId).orElse(null);
        if (tree == null) {
            return false;
        }

        // Owner has all permissions
        if (tree.getOwner().getEmail().equals(userEmail)) {
            return true;
        }

        // Check if user has editor permission
        return tree.getPermissions().stream()
                .filter(p -> p.getUser().getEmail().equals(userEmail))
                .anyMatch(p -> p.getRole() == PermissionRole.EDITOR || p.getRole() == PermissionRole.OWNER);
    }

    /**
     * Check if user is the owner of the tree
     */
    public boolean isOwner(UUID treeId, String userEmail) {
        FamilyTree tree = treeRepository.findById(treeId).orElse(null);
        if (tree == null) {
            return false;
        }

        return tree.getOwner().getEmail().equals(userEmail);
    }

    /**
     * Check if user has any access to the tree
     */
    public boolean hasAccess(UUID treeId, String userEmail) {
        return hasViewPermission(treeId, userEmail);
    }

    /**
     * Get the permission role for a user on a tree
     */
    public PermissionRole getPermissionRole(UUID treeId, String userEmail) {
        FamilyTree tree = treeRepository.findById(treeId).orElse(null);
        if (tree == null) {
            return null;
        }

        // Owner has owner role
        if (tree.getOwner().getEmail().equals(userEmail)) {
            return PermissionRole.OWNER;
        }

        // Get user's permission role
        return tree.getPermissions().stream()
                .filter(p -> p.getUser().getEmail().equals(userEmail))
                .map(p -> p.getRole())
                .findFirst()
                .orElse(null);
    }

    /**
     * Check if user can view a tree (by user ID)
     */
    public boolean canViewTree(UUID userId, UUID treeId) {
        FamilyTree tree = treeRepository.findById(treeId).orElse(null);
        if (tree == null) {
            return false;
        }

        // Owner has all permissions
        if (tree.getOwner().getId().equals(userId)) {
            return true;
        }

        // Check if user has any permission
        return tree.getPermissions().stream()
                .anyMatch(p -> p.getUser().getId().equals(userId));
    }

    /**
     * Check if user can modify a tree (by user ID)
     */
    public boolean canModifyTree(UUID userId, UUID treeId) {
        FamilyTree tree = treeRepository.findById(treeId).orElse(null);
        if (tree == null) {
            return false;
        }

        // Owner has all permissions
        if (tree.getOwner().getId().equals(userId)) {
            return true;
        }

        // Check if user has editor permission
        return tree.getPermissions().stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .anyMatch(p -> p.getRole() == PermissionRole.EDITOR || p.getRole() == PermissionRole.OWNER);
    }

    /**
     * Get all collaborators for a tree
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> getTreeCollaborators(UUID treeId, UUID userId) {
        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Tree not found"));

        // Verify user has access to view collaborators
        if (!canViewTree(userId, treeId)) {
            throw new RuntimeException("You don't have permission to view this tree");
        }

        List<PermissionResponse> collaborators = new ArrayList<>();

        // Add owner
        PermissionResponse owner = PermissionResponse.builder()
                .id(tree.getOwner().getId().toString())
                .userId(tree.getOwner().getId().toString())
                .userName(tree.getOwner().getName())
                .userEmail(tree.getOwner().getEmail())
                .role(PermissionRole.OWNER)
                .grantedAt(tree.getCreatedAt())
                .isOwner(true)
                .build();
        collaborators.add(owner);

        // Add other collaborators
        List<PermissionResponse> others = tree.getPermissions().stream()
                .map(PermissionResponse::fromEntity)
                .collect(Collectors.toList());
        collaborators.addAll(others);

        return collaborators;
    }

    /**
     * Update a collaborator's role
     */
    @Transactional
    public void updateCollaboratorRole(UUID treeId, UUID userId, UUID collaboratorId, PermissionRole newRole) {
        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Tree not found"));

        // Only owner can update roles
        if (!tree.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Only the tree owner can update collaborator roles");
        }

        // Cannot update to OWNER role
        if (newRole == PermissionRole.OWNER) {
            throw new RuntimeException("Cannot promote collaborator to owner");
        }

        // Find the permission
        TreePermission permission = tree.getPermissions().stream()
                .filter(p -> p.getUser().getId().equals(collaboratorId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Collaborator not found"));

        permission.setRole(newRole);
        permissionRepository.save(permission);

        log.info("Updated role for user {} on tree {} to {}", collaboratorId, treeId, newRole);
    }

    /**
     * Remove a collaborator
     */
    @Transactional
    public void removeCollaborator(UUID treeId, UUID userId, UUID collaboratorId) {
        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Tree not found"));

        // Only owner can remove collaborators
        if (!tree.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Only the tree owner can remove collaborators");
        }

        // Cannot remove owner
        if (tree.getOwner().getId().equals(collaboratorId)) {
            throw new RuntimeException("Cannot remove the tree owner");
        }

        // Find and delete the permission
        TreePermission permission = tree.getPermissions().stream()
                .filter(p -> p.getUser().getId().equals(collaboratorId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Collaborator not found"));

        permissionRepository.delete(permission);

        log.info("Removed user {} from tree {}", collaboratorId, treeId);
    }

    /**
     * Leave a tree (remove yourself as collaborator)
     */
    @Transactional
    public void leaveTree(UUID treeId, UUID userId) {
        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Tree not found"));

        // Cannot leave if you're the owner
        if (tree.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Tree owner cannot leave the tree");
        }

        // Find and delete the permission
        TreePermission permission = tree.getPermissions().stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("You don't have access to this tree"));

        permissionRepository.delete(permission);

        log.info("User {} left tree {}", userId, treeId);
    }
}

