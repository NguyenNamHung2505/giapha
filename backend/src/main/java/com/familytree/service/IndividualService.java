package com.familytree.service;

import com.familytree.dto.individual.CreateIndividualRequest;
import com.familytree.dto.individual.IndividualResponse;
import com.familytree.dto.individual.UpdateIndividualRequest;
import com.familytree.exception.ResourceNotFoundException;
import com.familytree.exception.UnauthorizedException;
import com.familytree.model.FamilyTree;
import com.familytree.model.Individual;
import com.familytree.model.User;
import com.familytree.repository.FamilyTreeRepository;
import com.familytree.repository.IndividualRepository;
import com.familytree.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service for managing individuals in family trees
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IndividualService {

    private final IndividualRepository individualRepository;
    private final FamilyTreeRepository treeRepository;
    private final UserRepository userRepository;
    private final com.familytree.repository.MediaRepository mediaRepository;
    private final com.familytree.repository.EventRepository eventRepository;
    private final MinioService minioService;

    /**
     * Create a new individual in a tree
     */
    public IndividualResponse createIndividual(UUID treeId, CreateIndividualRequest request, String userEmail) {
        log.info("Creating individual in tree {} for user '{}'", treeId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization
        if (!hasAccess(tree, userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        Individual individual = Individual.builder()
                .tree(tree)
                .givenName(request.getGivenName())
                .surname(request.getSurname())
                .suffix(request.getSuffix())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .birthPlace(request.getBirthPlace())
                .deathDate(request.getDeathDate())
                .deathPlace(request.getDeathPlace())
                .biography(request.getBiography())
                .build();

        Individual savedIndividual = individualRepository.save(individual);
        log.info("Individual created with ID: {}", savedIndividual.getId());

        return convertToResponse(savedIndividual);
    }

    /**
     * Get an individual by ID
     */
    @Transactional(readOnly = true)
    public IndividualResponse getIndividual(UUID treeId, UUID individualId, String userEmail) {
        log.info("Fetching individual {} for user '{}'", individualId, userEmail);

        // Get individual with tree eagerly loaded
        Individual individual = individualRepository.findByIdWithTree(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with ID: " + individualId));

        // Check authorization
        if (!hasAccess(individual.getTree(), userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        return convertToResponse(individual);
    }

    /**
     * List all individuals in a tree
     */
    @Transactional(readOnly = true)
    public Page<IndividualResponse> listIndividuals(UUID treeId, String userEmail, Pageable pageable) {
        log.info("Listing individuals in tree {} for user '{}'", treeId, userEmail);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization
        if (!hasAccess(tree, userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        // Use regular query - tree will be loaded when accessed within transaction
        Page<Individual> individuals = individualRepository.findByTree(tree, pageable);

        return individuals.map(this::convertToResponse);
    }

    /**
     * Search individuals by name
     */
    @Transactional(readOnly = true)
    public Page<IndividualResponse> searchIndividuals(UUID treeId, String searchTerm, String userEmail, Pageable pageable) {
        log.info("Searching individuals in tree {} with term '{}' for user '{}'", treeId, searchTerm, userEmail);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization
        if (!hasAccess(tree, userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        Page<Individual> individuals = individualRepository.searchByName(treeId, searchTerm, pageable);

        return individuals.map(this::convertToResponse);
    }

    /**
     * Update an individual
     */
    public IndividualResponse updateIndividual(UUID individualId, UpdateIndividualRequest request, String userEmail) {
        log.info("Updating individual {} for user '{}'", individualId, userEmail);

        Individual individual = individualRepository.findById(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with ID: " + individualId));

        // Check authorization - only owner can update
        if (!isOwner(individual.getTree(), userEmail)) {
            throw new UnauthorizedException("Only the tree owner can update individuals");
        }

        individual.setGivenName(request.getGivenName());
        individual.setSurname(request.getSurname());
        individual.setSuffix(request.getSuffix());
        individual.setGender(request.getGender());
        individual.setBirthDate(request.getBirthDate());
        individual.setBirthPlace(request.getBirthPlace());
        individual.setDeathDate(request.getDeathDate());
        individual.setDeathPlace(request.getDeathPlace());
        individual.setBiography(request.getBiography());

        Individual updatedIndividual = individualRepository.save(individual);
        log.info("Individual {} updated successfully", individualId);

        return convertToResponse(updatedIndividual);
    }

    /**
     * Delete an individual
     */
    public void deleteIndividual(UUID individualId, String userEmail) {
        log.info("Deleting individual {} for user '{}'", individualId, userEmail);

        Individual individual = individualRepository.findById(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with ID: " + individualId));

        // Check authorization - only owner can delete
        if (!isOwner(individual.getTree(), userEmail)) {
            throw new UnauthorizedException("Only the tree owner can delete individuals");
        }

        // Delete avatar from MinIO if exists
        if (individual.getProfilePictureUrl() != null && !individual.getProfilePictureUrl().isEmpty()) {
            try {
                String objectName = extractObjectNameFromUrl(individual.getProfilePictureUrl());
                if (objectName != null) {
                    minioService.deleteFile(objectName);
                    log.info("Deleted avatar for individual {}", individualId);
                }
            } catch (Exception e) {
                log.error("Failed to delete avatar for individual {}: {}", individualId, e.getMessage());
            }
        }

        individualRepository.delete(individual);
        log.info("Individual {} deleted successfully", individualId);
    }

    /**
     * Upload profile picture/avatar for an individual
     */
    public String uploadAvatar(UUID individualId, MultipartFile file, String userEmail) {
        log.info("Uploading avatar for individual {} by user '{}'", individualId, userEmail);

        Individual individual = individualRepository.findById(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with ID: " + individualId));

        // Check authorization - only owner can upload avatar
        if (!isOwner(individual.getTree(), userEmail)) {
            throw new UnauthorizedException("Only the tree owner can upload avatars");
        }

        try {
            // Delete old avatar if exists
            if (individual.getProfilePictureUrl() != null && !individual.getProfilePictureUrl().isEmpty()) {
                try {
                    String oldObjectName = extractObjectNameFromUrl(individual.getProfilePictureUrl());
                    if (oldObjectName != null) {
                        minioService.deleteFile(oldObjectName);
                        log.info("Deleted old avatar for individual {}", individualId);
                    }
                } catch (Exception e) {
                    log.warn("Failed to delete old avatar: {}", e.getMessage());
                }
            }

            // Generate object name: avatars/individuals/{id}/avatar.{extension}
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String objectName = "avatars/individuals/" + individualId + "/avatar" + extension;

            // Upload new avatar to MinIO
            String storagePath = minioService.uploadFile(file, objectName);

            // Generate URL: /api/trees/{treeId}/individuals/{id}/avatar
            String avatarUrl = "/api/trees/" + individual.getTree().getId() + "/individuals/" + individualId + "/avatar";

            // Update individual with new avatar URL
            individual.setProfilePictureUrl(avatarUrl);
            individualRepository.save(individual);

            log.info("Avatar uploaded successfully for individual {}: {}", individualId, avatarUrl);
            return avatarUrl;

        } catch (Exception e) {
            log.error("Failed to upload avatar for individual {}: {}", individualId, e.getMessage());
            throw new RuntimeException("Failed to upload avatar: " + e.getMessage(), e);
        }
    }

    /**
     * Delete profile picture/avatar for an individual
     */
    public void deleteAvatar(UUID individualId, String userEmail) {
        log.info("Deleting avatar for individual {} by user '{}'", individualId, userEmail);

        Individual individual = individualRepository.findById(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with ID: " + individualId));

        // Check authorization - only owner can delete avatar
        if (!isOwner(individual.getTree(), userEmail)) {
            throw new UnauthorizedException("Only the tree owner can delete avatars");
        }

        // Delete avatar from MinIO if exists
        if (individual.getProfilePictureUrl() != null && !individual.getProfilePictureUrl().isEmpty()) {
            try {
                String objectName = extractObjectNameFromUrl(individual.getProfilePictureUrl());
                if (objectName != null) {
                    minioService.deleteFile(objectName);
                    log.info("Deleted avatar from MinIO for individual {}", individualId);
                }
            } catch (Exception e) {
                log.error("Failed to delete avatar from MinIO: {}", e.getMessage());
                throw new RuntimeException("Failed to delete avatar: " + e.getMessage(), e);
            }
        }

        // Update individual to remove avatar URL
        individual.setProfilePictureUrl(null);
        individualRepository.save(individual);

        log.info("Avatar deleted successfully for individual {}", individualId);
    }

    /**
     * Extract object name from URL stored in profilePictureUrl
     * For avatars, we store API endpoint: /api/trees/{treeId}/individuals/{id}/avatar
     * And need to extract MinIO object name: avatars/individuals/{id}/avatar.{ext}
     * We need to query the individual to get the actual storage path
     */
    private String extractObjectNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        // Extract individual ID from URL: /api/trees/{treeId}/individuals/{id}/avatar
        String pattern = "/individuals/";
        int individualIdStart = url.indexOf(pattern);
        if (individualIdStart != -1) {
            individualIdStart += pattern.length();
            int individualIdEnd = url.indexOf("/", individualIdStart);
            if (individualIdEnd != -1) {
                String individualIdStr = url.substring(individualIdStart, individualIdEnd);
                try {
                    UUID individualId = UUID.fromString(individualIdStr);
                    // List files in MinIO for this individual to find avatar
                    String prefix = "avatars/individuals/" + individualId + "/";
                    List<String> files = minioService.listFiles(prefix);
                    if (!files.isEmpty()) {
                        // Return the first file found (should only be one avatar)
                        return files.get(0);
                    }
                } catch (Exception e) {
                    log.error("Failed to extract individual ID from URL: {}", url, e);
                }
            }
        }

        return null;
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
     * Convert Individual entity to IndividualResponse DTO
     */
    private IndividualResponse convertToResponse(Individual individual) {
        String fullName = buildFullName(individual);

        // Use count queries to avoid loading collections
        long mediaCount = mediaRepository.countByIndividualId(individual.getId());
        long eventCount = eventRepository.countByIndividualId(individual.getId());

        return IndividualResponse.builder()
                .id(individual.getId())
                .treeId(individual.getTree().getId())
                .treeName(individual.getTree().getName())
                .givenName(individual.getGivenName())
                .surname(individual.getSurname())
                .suffix(individual.getSuffix())
                .fullName(fullName)
                .gender(individual.getGender())
                .birthDate(individual.getBirthDate())
                .birthPlace(individual.getBirthPlace())
                .deathDate(individual.getDeathDate())
                .deathPlace(individual.getDeathPlace())
                .biography(individual.getBiography())
                .profilePictureUrl(individual.getProfilePictureUrl())
                .mediaCount((int) mediaCount)
                .eventCount((int) eventCount)
                .createdAt(individual.getCreatedAt())
                .updatedAt(individual.getUpdatedAt())
                .build();
    }

    /**
     * Build full name from individual parts
     */
    private String buildFullName(Individual individual) {
        StringBuilder fullName = new StringBuilder();

        if (individual.getGivenName() != null && !individual.getGivenName().isEmpty()) {
            fullName.append(individual.getGivenName());
        }

        if (individual.getSurname() != null && !individual.getSurname().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(individual.getSurname());
        }

        if (individual.getSuffix() != null && !individual.getSuffix().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(individual.getSuffix());
        }

        return fullName.length() > 0 ? fullName.toString() : "Unknown";
    }
}
