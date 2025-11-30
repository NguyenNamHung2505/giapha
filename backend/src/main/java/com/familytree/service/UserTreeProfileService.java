package com.familytree.service;

import com.familytree.dto.user.UserTreeProfileResponse;
import com.familytree.exception.BadRequestException;
import com.familytree.exception.ResourceNotFoundException;
import com.familytree.exception.UnauthorizedException;
import com.familytree.model.FamilyTree;
import com.familytree.model.Individual;
import com.familytree.model.User;
import com.familytree.model.UserTreeProfile;
import com.familytree.repository.FamilyTreeRepository;
import com.familytree.repository.IndividualRepository;
import com.familytree.repository.UserRepository;
import com.familytree.repository.UserTreeProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user-to-individual mappings in family trees
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserTreeProfileService {

    private final UserTreeProfileRepository userTreeProfileRepository;
    private final UserRepository userRepository;
    private final FamilyTreeRepository treeRepository;
    private final IndividualRepository individualRepository;

    /**
     * Link the current user to an individual in a tree
     */
    public UserTreeProfileResponse linkUserToIndividual(UUID treeId, UUID individualId, String userEmail) {
        log.info("Linking user '{}' to individual {} in tree {}", userEmail, individualId, treeId);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization - user must have access to the tree
        if (!hasAccess(tree, userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        Individual individual = individualRepository.findById(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with ID: " + individualId));

        // Verify individual belongs to the tree
        if (!individual.getTree().getId().equals(treeId)) {
            throw new BadRequestException("Individual does not belong to this tree");
        }

        // Check if mapping already exists, update it if so
        Optional<UserTreeProfile> existingProfile = userTreeProfileRepository.findByUserIdAndTreeId(user.getId(), treeId);

        UserTreeProfile profile;
        if (existingProfile.isPresent()) {
            profile = existingProfile.get();
            profile.setIndividual(individual);
            log.info("Updated existing profile mapping for user '{}' to individual {}", userEmail, individualId);
        } else {
            profile = UserTreeProfile.builder()
                    .user(user)
                    .tree(tree)
                    .individual(individual)
                    .build();
            log.info("Created new profile mapping for user '{}' to individual {}", userEmail, individualId);
        }

        UserTreeProfile savedProfile = userTreeProfileRepository.save(profile);
        return convertToResponse(savedProfile);
    }

    /**
     * Get the current user's profile mapping for a tree
     */
    @Transactional(readOnly = true)
    public UserTreeProfileResponse getUserProfile(UUID treeId, String userEmail) {
        log.info("Getting user profile for '{}' in tree {}", userEmail, treeId);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization
        if (!hasAccess(tree, userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        UserTreeProfile profile = userTreeProfileRepository.findByUserEmailAndTreeId(userEmail, treeId)
                .orElseThrow(() -> new ResourceNotFoundException("No profile mapping found for this user in this tree"));

        return convertToResponse(profile);
    }

    /**
     * Get the current user's profile mapping for a tree (returns null if not found)
     */
    @Transactional(readOnly = true)
    public UserTreeProfileResponse getUserProfileOrNull(UUID treeId, String userEmail) {
        log.info("Getting user profile (optional) for '{}' in tree {}", userEmail, treeId);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization
        if (!hasAccess(tree, userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        return userTreeProfileRepository.findByUserEmailAndTreeId(userEmail, treeId)
                .map(this::convertToResponse)
                .orElse(null);
    }

    /**
     * Remove the current user's profile mapping for a tree
     */
    public void unlinkUserFromIndividual(UUID treeId, String userEmail) {
        log.info("Unlinking user '{}' from tree {}", userEmail, treeId);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization
        if (!hasAccess(tree, userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        userTreeProfileRepository.deleteByUserIdAndTreeId(user.getId(), treeId);
        log.info("Removed profile mapping for user '{}' in tree {}", userEmail, treeId);
    }

    /**
     * Check if user has access to the tree (owner, has permission, admin, or linked via UserTreeProfile)
     */
    private boolean hasAccess(FamilyTree tree, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElse(null);
        if (user == null) {
            return false;
        }

        // Admin users have access to all trees
        if (user.isAdmin()) {
            return true;
        }

        // Owner has access
        if (tree.getOwner().getEmail().equals(userEmail)) {
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
        if (tree.getSourceTreeId() != null) {
            FamilyTree sourceTree = treeRepository.findById(tree.getSourceTreeId()).orElse(null);
            if (sourceTree != null) {
                if (userTreeProfileRepository.existsByUserIdAndTreeId(user.getId(), sourceTree.getId())) {
                    return true;
                }
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
     * Convert entity to response DTO
     */
    private UserTreeProfileResponse convertToResponse(UserTreeProfile profile) {
        Individual individual = profile.getIndividual();

        return UserTreeProfileResponse.builder()
                .id(profile.getId())
                .treeId(profile.getTree().getId())
                .treeName(profile.getTree().getName())
                .individual(UserTreeProfileResponse.IndividualInfo.builder()
                        .id(individual.getId())
                        .givenName(individual.getGivenName())
                        .surname(individual.getSurname())
                        .fullName(buildFullName(individual))
                        .gender(individual.getGender() != null ? individual.getGender().name() : null)
                        .birthDate(individual.getBirthDate())
                        .profilePictureUrl(individual.getProfilePictureUrl())
                        .build())
                .createdAt(profile.getCreatedAt())
                .build();
    }

    /**
     * Build full name - Vietnamese name order: Surname + Suffix + Given Name
     */
    private String buildFullName(Individual individual) {
        StringBuilder fullName = new StringBuilder();
        if (individual.getSurname() != null && !individual.getSurname().isEmpty()) {
            fullName.append(individual.getSurname());
        }
        if (individual.getSuffix() != null && !individual.getSuffix().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(individual.getSuffix());
        }
        if (individual.getGivenName() != null && !individual.getGivenName().isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(individual.getGivenName());
        }
        return fullName.length() > 0 ? fullName.toString() : "Unknown";
    }
}
