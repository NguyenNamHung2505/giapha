package com.familytree.service;

import com.familytree.model.FamilyTree;
import com.familytree.model.PermissionRole;
import com.familytree.repository.FamilyTreeRepository;
import com.familytree.repository.TreePermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for managing tree permissions and authorization checks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final FamilyTreeRepository treeRepository;
    private final TreePermissionRepository permissionRepository;

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
}

