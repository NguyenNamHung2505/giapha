package com.familytree.service;

import com.familytree.dto.tree.CreateTreeRequest;
import com.familytree.dto.tree.TreeResponse;
import com.familytree.dto.tree.UpdateTreeRequest;
import com.familytree.exception.ResourceNotFoundException;
import com.familytree.exception.UnauthorizedException;
import com.familytree.model.FamilyTree;
import com.familytree.model.Individual;
import com.familytree.model.Media;
import com.familytree.model.User;
import com.familytree.repository.FamilyTreeRepository;
import com.familytree.repository.IndividualCloneMappingRepository;
import com.familytree.repository.MediaRepository;
import com.familytree.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
    private final com.familytree.repository.IndividualRepository individualRepository;
    private final com.familytree.repository.RelationshipRepository relationshipRepository;
    private final com.familytree.repository.UserTreeProfileRepository userTreeProfileRepository;
    private final com.familytree.repository.TreePermissionRepository treePermissionRepository;
    private final com.familytree.repository.TreeInvitationRepository treeInvitationRepository;
    private final MediaRepository mediaRepository;
    private final IndividualCloneMappingRepository cloneMappingRepository;
    private final MinioService minioService;

    /**
     * Create a new family tree
     * Only system admins can create new trees
     */
    public TreeResponse createTree(CreateTreeRequest request, String userEmail) {
        log.info("Creating tree '{}' for user '{}'", request.getName(), userEmail);

        User owner = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Only system admin can create new trees
        if (!owner.isAdmin()) {
            throw new UnauthorizedException("Only system administrators can create new family trees");
        }

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
     * Admin users see all trees, regular users see trees they own, have permission for, or are linked to
     */
    @Transactional(readOnly = true)
    public Page<TreeResponse> listTrees(String userEmail, Pageable pageable) {
        log.info("Listing trees for user '{}'", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<FamilyTree> trees;
        if (user.isAdmin()) {
            log.info("User '{}' is admin, returning all trees", userEmail);
            trees = treeRepository.findAllTrees(pageable);
        } else {
            trees = treeRepository.findTreesAccessibleByUser(user.getId(), pageable);
        }

        return trees.map(this::convertToResponse);
    }

    /**
     * Update a tree
     */
    public TreeResponse updateTree(UUID treeId, UpdateTreeRequest request, String userEmail) {
        log.info("Updating tree {} for user '{}'", treeId, userEmail);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization - owner or admin can update
        if (!canModify(tree, userEmail)) {
            throw new UnauthorizedException("Only the owner or admin can update this tree");
        }

        tree.setName(request.getName());
        tree.setDescription(request.getDescription());

        FamilyTree updatedTree = treeRepository.save(tree);
        log.info("Tree {} updated successfully", treeId);

        return convertToResponse(updatedTree);
    }

    /**
     * Delete a tree and all associated data (media files, relationships, individuals)
     */
    public void deleteTree(UUID treeId, String userEmail) {
        log.info("Deleting tree {} for user '{}'", treeId, userEmail);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization - owner or admin can delete
        if (!canModify(tree, userEmail)) {
            throw new UnauthorizedException("Only the owner or admin can delete this tree");
        }

        // 1. Delete clone mappings (both as source and cloned tree)
        cloneMappingRepository.deleteBySourceTreeId(treeId);
        cloneMappingRepository.deleteByClonedTreeId(treeId);
        log.info("Deleted clone mappings for tree {}", treeId);

        // 2. Delete user tree profiles (user-to-individual links)
        userTreeProfileRepository.deleteByTreeId(treeId);
        log.info("Deleted user tree profiles for tree {}", treeId);

        // 3. Delete tree permissions
        treePermissionRepository.deleteByTreeId(treeId);
        log.info("Deleted tree permissions for tree {}", treeId);

        // 4. Delete tree invitations
        treeInvitationRepository.deleteByTreeId(treeId);
        log.info("Deleted tree invitations for tree {}", treeId);

        // 5. Delete media files from MinIO and database
        deleteMediaFiles(treeId);

        // 6. Delete avatar files from MinIO
        deleteAvatarFiles(treeId);

        // 7. Delete relationships (must be before individuals due to FK constraints)
        relationshipRepository.deleteByTreeId(treeId);
        log.info("Deleted relationships for tree {}", treeId);

        // 8. Delete individuals
        individualRepository.deleteByTreeId(treeId);
        log.info("Deleted individuals for tree {}", treeId);

        // 9. Delete the tree itself
        treeRepository.delete(tree);
        log.info("Tree {} deleted successfully", treeId);
    }

    /**
     * Delete all media files (from MinIO and database) for a tree
     */
    private void deleteMediaFiles(UUID treeId) {
        List<Media> mediaList = mediaRepository.findAllByTreeId(treeId);
        log.info("Deleting {} media files for tree {}", mediaList.size(), treeId);

        for (Media media : mediaList) {
            try {
                // Delete original file
                minioService.deleteFile(media.getStoragePath());

                // Delete thumbnail if it's an image
                if (media.getMimeType() != null && media.getMimeType().startsWith("image/")) {
                    String thumbnailPath = minioService.generateThumbnailName(media.getStoragePath());
                    if (minioService.fileExists(thumbnailPath)) {
                        minioService.deleteFile(thumbnailPath);
                    }
                }

                // Delete from database
                mediaRepository.delete(media);
            } catch (Exception e) {
                log.warn("Failed to delete media file {}: {}", media.getStoragePath(), e.getMessage());
                // Continue with other files
            }
        }
    }

    /**
     * Delete avatar files from MinIO for all individuals in a tree
     */
    private void deleteAvatarFiles(UUID treeId) {
        List<Individual> individuals = individualRepository.findByTreeId(treeId);

        for (Individual individual : individuals) {
            if (individual.getProfilePictureUrl() != null) {
                try {
                    // Avatar is stored at: avatars/individuals/{id}/avatar.{extension}
                    String avatarBasePath = "avatars/individuals/" + individual.getId() + "/";
                    minioService.deleteDirectory(avatarBasePath);
                } catch (Exception e) {
                    log.warn("Failed to delete avatar for individual {}: {}", individual.getId(), e.getMessage());
                    // Continue with other avatars
                }
            }
        }
        log.info("Deleted avatar files for tree {}", treeId);
    }

    /**
     * Check if user has access to the tree (owner, tree admin, has permission, system admin, or linked via UserTreeProfile)
     */
    private boolean hasAccess(FamilyTree tree, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return false;
        }

        // System admin users have access to all trees
        if (user.isAdmin()) {
            return true;
        }

        // Owner has access
        if (tree.getOwner().getEmail().equals(userEmail)) {
            return true;
        }

        // Tree Admin has access (check all admins)
        if (tree.getAdmins() != null && tree.getAdmins().stream()
                .anyMatch(admin -> admin.getEmail().equals(userEmail))) {
            return true;
        }

        // User with permission has access
        boolean hasPermission = tree.getPermissions().stream()
                .anyMatch(p -> p.getUser().getEmail().equals(userEmail));
        if (hasPermission) {
            return true;
        }

        // User linked via UserTreeProfile has access
        if (userTreeProfileRepository.existsByUserIdAndTreeId(user.getId(), tree.getId())) {
            return true;
        }

        // If this is a cloned tree, check if user has access to the source tree
        // This allows relatives of the cloned person to view the cloned tree
        if (tree.getSourceTreeId() != null) {
            FamilyTree sourceTree = treeRepository.findById(tree.getSourceTreeId()).orElse(null);
            if (sourceTree != null) {
                // Check if user has profile in source tree (they are a family member)
                if (userTreeProfileRepository.existsByUserIdAndTreeId(user.getId(), sourceTree.getId())) {
                    return true;
                }
                // Also check if user is owner or has permission on source tree
                if (sourceTree.getOwner().getEmail().equals(userEmail)) {
                    return true;
                }
                boolean hasSourcePermission = sourceTree.getPermissions().stream()
                        .anyMatch(p -> p.getUser().getEmail().equals(userEmail));
                if (hasSourcePermission) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Check if user is the owner of the tree
     */
    private boolean isOwner(FamilyTree tree, String userEmail) {
        return tree.getOwner().getEmail().equals(userEmail);
    }

    /**
     * Check if user is a tree admin
     */
    private boolean isTreeAdmin(FamilyTree tree, String userEmail) {
        return tree.getAdmins() != null && tree.getAdmins().stream()
                .anyMatch(admin -> admin.getEmail().equals(userEmail));
    }

    /**
     * Add a tree admin
     * Only the owner can add tree admins
     */
    public TreeResponse addTreeAdmin(UUID treeId, UUID adminUserId, String requestingUserEmail) {
        log.info("Adding tree admin for tree {} user {} by {}", treeId, adminUserId, requestingUserEmail);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Only owner can add tree admin
        if (!isOwner(tree, requestingUserEmail)) {
            throw new UnauthorizedException("Only the tree owner can add tree admins");
        }

        User newAdmin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + adminUserId));

        // Cannot add owner as admin
        if (tree.getOwner().getId().equals(adminUserId)) {
            throw new UnauthorizedException("Cannot add owner as admin - owner already has full permissions");
        }

        // Check if already an admin
        if (tree.getAdmins().stream().anyMatch(a -> a.getId().equals(adminUserId))) {
            log.info("User {} is already an admin of tree {}", adminUserId, treeId);
            return convertToResponse(tree);
        }

        tree.getAdmins().add(newAdmin);
        log.info("Tree admin added: user {} for tree {}", newAdmin.getEmail(), treeId);

        FamilyTree updatedTree = treeRepository.save(tree);
        return convertToResponse(updatedTree);
    }

    /**
     * Remove a tree admin
     * Only the owner can remove tree admins
     */
    public TreeResponse removeTreeAdmin(UUID treeId, UUID adminUserId, String requestingUserEmail) {
        log.info("Removing tree admin {} from tree {} by {}", adminUserId, treeId, requestingUserEmail);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Only owner can remove tree admin
        if (!isOwner(tree, requestingUserEmail)) {
            throw new UnauthorizedException("Only the tree owner can remove tree admins");
        }

        tree.getAdmins().removeIf(admin -> admin.getId().equals(adminUserId));
        log.info("Tree admin removed: user {} from tree {}", adminUserId, treeId);

        FamilyTree updatedTree = treeRepository.save(tree);
        return convertToResponse(updatedTree);
    }

    /**
     * Check if user can modify tree (owner, tree admin, or system admin)
     */
    private boolean canModify(FamilyTree tree, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return false;
        }

        // System admin users can modify all trees
        if (user.isAdmin()) {
            return true;
        }

        // Owner can modify
        if (tree.getOwner().getEmail().equals(userEmail)) {
            return true;
        }

        // Tree Admins can modify
        if (tree.getAdmins() != null && tree.getAdmins().stream()
                .anyMatch(admin -> admin.getEmail().equals(userEmail))) {
            return true;
        }

        return false;
    }

    /**
     * Convert FamilyTree entity to TreeResponse DTO
     */
    private TreeResponse convertToResponse(FamilyTree tree) {
        // Use count queries to avoid loading collections
        long individualsCount = individualRepository.countByTreeId(tree.getId());
        long relationshipsCount = relationshipRepository.countByTreeId(tree.getId());

        // Get root individual name if exists
        String rootIndividualName = null;
        if (tree.getRootIndividualId() != null) {
            rootIndividualName = individualRepository.findById(tree.getRootIndividualId())
                    .map(ind -> {
                        StringBuilder name = new StringBuilder();
                        if (ind.getGivenName() != null) name.append(ind.getGivenName());
                        if (ind.getSurname() != null) {
                            if (name.length() > 0) name.insert(0, " ");
                            name.insert(0, ind.getSurname());
                        }
                        return name.toString();
                    })
                    .orElse(null);
        }

        // Build admins list
        List<TreeResponse.AdminInfo> adminInfoList = new java.util.ArrayList<>();
        if (tree.getAdmins() != null) {
            for (User admin : tree.getAdmins()) {
                adminInfoList.add(TreeResponse.AdminInfo.builder()
                        .id(admin.getId())
                        .name(admin.getName())
                        .email(admin.getEmail())
                        .build());
            }
        }

        return TreeResponse.builder()
                .id(tree.getId())
                .name(tree.getName())
                .description(tree.getDescription())
                .ownerId(tree.getOwner().getId())
                .ownerName(tree.getOwner().getName())
                .ownerEmail(tree.getOwner().getEmail())
                .admins(adminInfoList)
                .individualsCount((int) individualsCount)
                .relationshipsCount((int) relationshipsCount)
                .rootIndividualId(tree.getRootIndividualId())
                .rootIndividualName(rootIndividualName)
                .createdAt(tree.getCreatedAt())
                .updatedAt(tree.getUpdatedAt())
                .build();
    }
}
