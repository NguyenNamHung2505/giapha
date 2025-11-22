package com.familytree.service;

import com.familytree.dto.tree.CreateTreeRequest;
import com.familytree.dto.tree.TreeResponse;
import com.familytree.dto.tree.UpdateTreeRequest;
import com.familytree.exception.ResourceNotFoundException;
import com.familytree.exception.UnauthorizedException;
import com.familytree.model.FamilyTree;
import com.familytree.model.User;
import com.familytree.repository.FamilyTreeRepository;
import com.familytree.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for managing family trees
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TreeService {

    private final FamilyTreeRepository treeRepository;
    private final UserRepository userRepository;

    /**
     * Create a new family tree
     */
    public TreeResponse createTree(CreateTreeRequest request, String userEmail) {
        log.info("Creating tree '{}' for user '{}'", request.getName(), userEmail);

        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FamilyTree tree = FamilyTree.builder()
                .name(request.getName())
                .description(request.getDescription())
                .owner(owner)
                .build();

        FamilyTree savedTree = treeRepository.save(tree);
        log.info("Tree created with ID: {}", savedTree.getId());

        return convertToResponse(savedTree);
    }

    /**
     * Get a tree by ID
     */
    @Transactional(readOnly = true)
    public TreeResponse getTree(UUID treeId, String userEmail) {
        log.info("Fetching tree {} for user '{}'", treeId, userEmail);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization
        if (!hasAccess(tree, userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        return convertToResponse(tree);
    }

    /**
     * List all trees accessible by the user
     */
    @Transactional(readOnly = true)
    public Page<TreeResponse> listTrees(String userEmail, Pageable pageable) {
        log.info("Listing trees for user '{}'", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<FamilyTree> trees = treeRepository.findTreesAccessibleByUser(user.getId(), pageable);

        return trees.map(this::convertToResponse);
    }

    /**
     * Update a tree
     */
    public TreeResponse updateTree(UUID treeId, UpdateTreeRequest request, String userEmail) {
        log.info("Updating tree {} for user '{}'", treeId, userEmail);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization - only owner can update
        if (!isOwner(tree, userEmail)) {
            throw new UnauthorizedException("Only the owner can update this tree");
        }

        tree.setName(request.getName());
        tree.setDescription(request.getDescription());

        FamilyTree updatedTree = treeRepository.save(tree);
        log.info("Tree {} updated successfully", treeId);

        return convertToResponse(updatedTree);
    }

    /**
     * Delete a tree
     */
    public void deleteTree(UUID treeId, String userEmail) {
        log.info("Deleting tree {} for user '{}'", treeId, userEmail);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization - only owner can delete
        if (!isOwner(tree, userEmail)) {
            throw new UnauthorizedException("Only the owner can delete this tree");
        }

        treeRepository.delete(tree);
        log.info("Tree {} deleted successfully", treeId);
    }

    /**
     * Check if user has access to the tree (owner or has permission)
     */
    private boolean hasAccess(FamilyTree tree, String userEmail) {
        if (tree.getOwner().getEmail().equals(userEmail)) {
            return true;
        }
        return tree.getPermissions().stream()
                .anyMatch(p -> p.getUser().getEmail().equals(userEmail));
    }

    /**
     * Check if user is the owner of the tree
     */
    private boolean isOwner(FamilyTree tree, String userEmail) {
        return tree.getOwner().getEmail().equals(userEmail);
    }

    /**
     * Convert FamilyTree entity to TreeResponse DTO
     */
    private TreeResponse convertToResponse(FamilyTree tree) {
        return TreeResponse.builder()
                .id(tree.getId())
                .name(tree.getName())
                .description(tree.getDescription())
                .ownerId(tree.getOwner().getId())
                .ownerName(tree.getOwner().getName())
                .ownerEmail(tree.getOwner().getEmail())
                .individualsCount(tree.getIndividuals() != null ? tree.getIndividuals().size() : 0)
                .relationshipsCount(tree.getRelationships() != null ? tree.getRelationships().size() : 0)
                .createdAt(tree.getCreatedAt())
                .updatedAt(tree.getUpdatedAt())
                .build();
    }
}
