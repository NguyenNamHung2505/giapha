package com.familytree.service;

import com.familytree.dto.clone.IndividualCloneInfoResponse;
import com.familytree.dto.clone.TreeCloneInfoResponse;
import com.familytree.dto.tree.CreateTreeFromIndividualRequest;
import com.familytree.dto.tree.CreateTreeFromIndividualResponse;
import com.familytree.exception.BadRequestException;
import com.familytree.exception.ResourceNotFoundException;
import com.familytree.exception.UnauthorizedException;
import com.familytree.model.*;
import com.familytree.repository.*;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for cloning family trees from a selected individual
 * Creates a new tree with all ancestors and descendants of the selected person
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TreeCloneService {

    private final FamilyTreeRepository treeRepository;
    private final IndividualRepository individualRepository;
    private final RelationshipRepository relationshipRepository;
    private final MediaRepository mediaRepository;
    private final UserRepository userRepository;
    private final UserTreeProfileRepository userTreeProfileRepository;
    private final IndividualCloneMappingRepository cloneMappingRepository;
    private final MinioService minioService;

    /**
     * Create a new family tree from a selected individual
     * Clones all ancestors, descendants, and their spouses
     * Only system admins can clone trees
     */
    @Transactional
    public CreateTreeFromIndividualResponse createTreeFromIndividual(
            CreateTreeFromIndividualRequest request,
            String userEmail) {

        log.info("Creating new tree from individual {} in tree {} by user {}",
                request.getRootIndividualId(), request.getSourceTreeId(), userEmail);

        // 1. Validate user exists and is system admin
        User user = userRepository.findByEmail(userEmail)
                .orElseGet(() -> userRepository.findByUsername(userEmail)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        // Only system admin can clone trees
        if (!user.isAdmin()) {
            throw new UnauthorizedException("Only system administrators can create new family trees from individuals");
        }

        // 2. Validate and get source tree
        FamilyTree sourceTree = treeRepository.findById(request.getSourceTreeId())
                .orElseThrow(() -> new ResourceNotFoundException("Source tree not found"));

        // 3. Validate user has access to source tree
        if (!hasAccess(sourceTree, user)) {
            throw new UnauthorizedException("You don't have access to this tree");
        }

        // 4. Validate root individual exists in source tree
        Individual rootIndividual = individualRepository.findById(request.getRootIndividualId())
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found"));

        if (!rootIndividual.getTree().getId().equals(request.getSourceTreeId())) {
            throw new BadRequestException("Individual does not belong to the source tree");
        }

        // 5. Check if individual has already been exported
        boolean alreadyExported = treeRepository.existsBySourceIndividualId(
                request.getRootIndividualId());
        if (alreadyExported) {
            log.warn("Individual {} has already been exported to another tree", request.getRootIndividualId());
            // We still allow it but log a warning - could throw exception if strict validation needed
        }

        // 6. Collect all individuals to clone (ancestors + descendants + spouses)
        Set<UUID> individualsToClone = collectAllRelatedIndividuals(request.getRootIndividualId());
        log.info("Found {} individuals to clone", individualsToClone.size());

        // 7. Create new tree
        // The cloner becomes both owner and admin of the new tree
        FamilyTree newTree = FamilyTree.builder()
                .owner(user)
                .name(request.getNewTreeName())
                .description(request.getNewTreeDescription())
                .sourceTreeId(request.getSourceTreeId())
                .sourceIndividualId(request.getRootIndividualId())
                .clonedAt(LocalDateTime.now())
                .build();
        // Add cloner as admin
        newTree.getAdmins().add(user);
        newTree = treeRepository.save(newTree);
        log.info("Created new tree with ID: {}, admin: {}", newTree.getId(), user.getEmail());

        // 8. Clone individuals and create ID mapping
        Map<UUID, Individual> idMapping = cloneIndividuals(individualsToClone, newTree);
        log.info("Cloned {} individuals", idMapping.size());

        // 9. Clone relationships
        List<Relationship> clonedRelationships = cloneRelationships(
                individualsToClone, idMapping, newTree);
        log.info("Cloned {} relationships", clonedRelationships.size());

        // 10. Clone media files if requested
        int totalMediaFiles = 0;
        if (request.isIncludeMedia()) {
            totalMediaFiles = cloneMedia(idMapping, newTree.getId());
            log.info("Cloned {} media files", totalMediaFiles);
        }

        // 11. Save clone mappings for navigation between trees
        saveCloneMappings(idMapping, sourceTree, newTree, request.getRootIndividualId());
        log.info("Saved {} clone mappings", idMapping.size());

        // 12. Copy user profile links from source tree to cloned tree
        int copiedProfiles = copyUserProfiles(idMapping, sourceTree, newTree);
        log.info("Copied {} user profile links", copiedProfiles);

        // 13. Get new root individual ID and set it as tree's root individual
        UUID newRootIndividualId = idMapping.get(request.getRootIndividualId()).getId();
        newTree.setRootIndividualId(newRootIndividualId);
        treeRepository.save(newTree);
        log.info("Set root individual ID {} for cloned tree", newRootIndividualId);

        return CreateTreeFromIndividualResponse.builder()
                .newTreeId(newTree.getId())
                .newTreeName(newTree.getName())
                .rootIndividualId(newRootIndividualId)
                .totalIndividuals(idMapping.size())
                .totalRelationships(clonedRelationships.size())
                .totalMediaFiles(totalMediaFiles)
                .sourceTreeId(request.getSourceTreeId())
                .sourceIndividualId(request.getRootIndividualId())
                .clonedAt(newTree.getClonedAt())
                .message("Successfully created new tree with " + idMapping.size() + " individuals")
                .build();
    }

    /**
     * Check if a tree has already been created from this individual
     */
    public boolean isIndividualAlreadyExported(UUID individualId) {
        return treeRepository.existsBySourceIndividualId(individualId);
    }

    /**
     * Get clone information for a tree
     * Shows if this tree is a clone or has been cloned, and all related trees
     */
    @Transactional(readOnly = true)
    public TreeCloneInfoResponse getTreeCloneInfo(UUID treeId) {
        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found"));

        boolean isClone = tree.getSourceTreeId() != null;
        TreeCloneInfoResponse.SourceTreeInfo sourceInfo = null;

        // If this tree is a clone, get source tree info
        if (isClone) {
            FamilyTree sourceTree = treeRepository.findById(tree.getSourceTreeId()).orElse(null);
            if (sourceTree != null) {
                Individual sourceIndividual = tree.getSourceIndividualId() != null ?
                        individualRepository.findById(tree.getSourceIndividualId()).orElse(null) : null;

                sourceInfo = TreeCloneInfoResponse.SourceTreeInfo.builder()
                        .sourceTreeId(sourceTree.getId())
                        .sourceTreeName(sourceTree.getName())
                        .sourceIndividualId(tree.getSourceIndividualId())
                        .sourceIndividualName(sourceIndividual != null ? buildFullName(sourceIndividual) : null)
                        .clonedAt(tree.getClonedAt())
                        .build();
            }
        }

        // Find all trees that were cloned from this tree
        List<FamilyTree> clonedFromThis = treeRepository.findBySourceTreeId(treeId);
        List<TreeCloneInfoResponse.ClonedTreeInfo> clonedTrees = clonedFromThis.stream()
                .map(clonedTree -> {
                    Individual rootIndividual = clonedTree.getSourceIndividualId() != null ?
                            // Find the cloned individual in the cloned tree
                            findClonedIndividual(treeId, clonedTree.getId(), clonedTree.getSourceIndividualId()) : null;

                    return TreeCloneInfoResponse.ClonedTreeInfo.builder()
                            .clonedTreeId(clonedTree.getId())
                            .clonedTreeName(clonedTree.getName())
                            .rootIndividualId(rootIndividual != null ? rootIndividual.getId() : null)
                            .rootIndividualName(rootIndividual != null ? buildFullName(rootIndividual) : null)
                            .clonedAt(clonedTree.getClonedAt())
                            .build();
                })
                .collect(Collectors.toList());

        // Build list of all related trees
        List<TreeCloneInfoResponse.RelatedTreeInfo> allRelatedTrees = buildAllRelatedTrees(tree, isClone, sourceInfo, clonedTrees);

        return TreeCloneInfoResponse.builder()
                .treeId(treeId)
                .treeName(tree.getName())
                .isClone(isClone)
                .hasClones(!clonedTrees.isEmpty())
                .sourceTreeInfo(sourceInfo)
                .clonedTrees(clonedTrees)
                .allRelatedTrees(allRelatedTrees)
                .build();
    }

    /**
     * Find the cloned individual in a cloned tree based on the source individual
     */
    private Individual findClonedIndividual(UUID sourceTreeId, UUID clonedTreeId, UUID sourceIndividualId) {
        // Use the clone mapping to find the corresponding individual
        List<IndividualCloneMapping> mappings = cloneMappingRepository.findBySourceIndividualId(sourceIndividualId);
        for (IndividualCloneMapping mapping : mappings) {
            if (mapping.getClonedTree().getId().equals(clonedTreeId)) {
                return mapping.getClonedIndividual();
            }
        }
        return null;
    }

    /**
     * Build list of all related trees for navigation
     */
    private List<TreeCloneInfoResponse.RelatedTreeInfo> buildAllRelatedTrees(
            FamilyTree currentTree,
            boolean isClone,
            TreeCloneInfoResponse.SourceTreeInfo sourceInfo,
            List<TreeCloneInfoResponse.ClonedTreeInfo> clonedTrees) {

        List<TreeCloneInfoResponse.RelatedTreeInfo> result = new ArrayList<>();

        // Add current tree
        result.add(TreeCloneInfoResponse.RelatedTreeInfo.builder()
                .treeId(currentTree.getId())
                .treeName(currentTree.getName())
                .isCurrentTree(true)
                .isSourceTree(!isClone && !clonedTrees.isEmpty())
                .clonedAt(currentTree.getClonedAt())
                .build());

        // Add source tree if this is a clone
        if (isClone && sourceInfo != null) {
            result.add(TreeCloneInfoResponse.RelatedTreeInfo.builder()
                    .treeId(sourceInfo.getSourceTreeId())
                    .treeName(sourceInfo.getSourceTreeName())
                    .isCurrentTree(false)
                    .isSourceTree(true)
                    .clonedAt(sourceInfo.getClonedAt())
                    .build());

            // Also add sibling clones (other trees cloned from the same source)
            List<FamilyTree> siblingClones = treeRepository.findBySourceTreeId(sourceInfo.getSourceTreeId());
            for (FamilyTree sibling : siblingClones) {
                if (!sibling.getId().equals(currentTree.getId())) {
                    result.add(TreeCloneInfoResponse.RelatedTreeInfo.builder()
                            .treeId(sibling.getId())
                            .treeName(sibling.getName())
                            .isCurrentTree(false)
                            .isSourceTree(false)
                            .clonedAt(sibling.getClonedAt())
                            .build());
                }
            }
        }

        // Add cloned trees
        for (TreeCloneInfoResponse.ClonedTreeInfo cloned : clonedTrees) {
            result.add(TreeCloneInfoResponse.RelatedTreeInfo.builder()
                    .treeId(cloned.getClonedTreeId())
                    .treeName(cloned.getClonedTreeName())
                    .isCurrentTree(false)
                    .isSourceTree(false)
                    .clonedAt(cloned.getClonedAt())
                    .build());
        }

        // Sort: current tree first, then source, then others
        result.sort((a, b) -> {
            if (a.isCurrentTree()) return -1;
            if (b.isCurrentTree()) return 1;
            if (a.isSourceTree()) return -1;
            if (b.isSourceTree()) return 1;
            return a.getTreeName().compareTo(b.getTreeName());
        });

        return result;
    }

    /**
     * Get clone information for an individual
     * This includes both trees this individual was cloned TO and source info if this is a clone
     */
    @Transactional(readOnly = true)
    public IndividualCloneInfoResponse getIndividualCloneInfo(UUID treeId, UUID individualId) {
        Individual individual = individualRepository.findById(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found"));

        String fullName = buildFullName(individual);
        FamilyTree currentTree = individual.getTree();

        // Check if this individual has been cloned to other trees
        List<IndividualCloneMapping> clonedToMappings = cloneMappingRepository.findBySourceIndividualId(individualId);

        // Check if this individual is a clone from another tree
        Optional<IndividualCloneMapping> sourceMapping = cloneMappingRepository.findByClonedIndividualId(individualId);

        List<IndividualCloneInfoResponse.ClonedTreeInfo> clonedToTrees = clonedToMappings.stream()
                .map(mapping -> IndividualCloneInfoResponse.ClonedTreeInfo.builder()
                        .clonedTreeId(mapping.getClonedTree().getId())
                        .clonedTreeName(mapping.getClonedTree().getName())
                        .clonedIndividualId(mapping.getClonedIndividual().getId())
                        .clonedIndividualName(buildFullName(mapping.getClonedIndividual()))
                        .isRootOfClone(mapping.isRootIndividual())
                        .clonedAt(mapping.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        IndividualCloneInfoResponse.SourceInfo sourceInfo = sourceMapping
                .map(mapping -> IndividualCloneInfoResponse.SourceInfo.builder()
                        .sourceTreeId(mapping.getSourceTree().getId())
                        .sourceTreeName(mapping.getSourceTree().getName())
                        .sourceIndividualId(mapping.getSourceIndividual().getId())
                        .sourceIndividualName(buildFullName(mapping.getSourceIndividual()))
                        .clonedAt(mapping.getCreatedAt())
                        .build())
                .orElse(null);

        // Build all tree locations for "View in Tree" dropdown
        List<IndividualCloneInfoResponse.TreeLocation> allTreeLocations = buildAllTreeLocations(
                individual, currentTree, clonedToMappings, sourceMapping);

        // Check if this person is the ROOT of a clone relationship
        // Either: was selected as root when creating clone (source side)
        // Or: is the root individual in a cloned tree (clone side)
        boolean isRootClonedPerson = clonedToMappings.stream().anyMatch(IndividualCloneMapping::isRootIndividual)
                || (sourceMapping.isPresent() && sourceMapping.get().isRootIndividual());

        return IndividualCloneInfoResponse.builder()
                .individualId(individualId)
                .individualName(fullName)
                .hasClones(!clonedToTrees.isEmpty())
                .isClone(sourceInfo != null)
                .isRootClonedPerson(isRootClonedPerson)
                .clonedToTrees(clonedToTrees)
                .sourceInfo(sourceInfo)
                .allTreeLocations(allTreeLocations)
                .build();
    }

    /**
     * Build list of all tree locations where this person exists
     */
    private List<IndividualCloneInfoResponse.TreeLocation> buildAllTreeLocations(
            Individual currentIndividual,
            FamilyTree currentTree,
            List<IndividualCloneMapping> clonedToMappings,
            Optional<IndividualCloneMapping> sourceMapping) {

        List<IndividualCloneInfoResponse.TreeLocation> locations = new ArrayList<>();
        UUID currentTreeId = currentTree.getId();
        boolean isCurrentAClone = sourceMapping.isPresent();

        // If current is a clone, add the source tree first
        if (isCurrentAClone) {
            IndividualCloneMapping source = sourceMapping.get();
            locations.add(IndividualCloneInfoResponse.TreeLocation.builder()
                    .treeId(source.getSourceTree().getId())
                    .treeName(source.getSourceTree().getName())
                    .individualId(source.getSourceIndividual().getId())
                    .isCurrentTree(false)
                    .isSourceTree(true)
                    .build());

            // Also find all clones from that source (siblings of current individual)
            List<IndividualCloneMapping> siblingClones = cloneMappingRepository
                    .findBySourceIndividualId(source.getSourceIndividual().getId());
            for (IndividualCloneMapping sibling : siblingClones) {
                UUID siblingTreeId = sibling.getClonedTree().getId();
                // Skip current tree (will be added below)
                if (!siblingTreeId.equals(currentTreeId)) {
                    locations.add(IndividualCloneInfoResponse.TreeLocation.builder()
                            .treeId(siblingTreeId)
                            .treeName(sibling.getClonedTree().getName())
                            .individualId(sibling.getClonedIndividual().getId())
                            .isCurrentTree(false)
                            .isSourceTree(false)
                            .build());
                }
            }
        } else {
            // Current is the source - add all cloned trees
            for (IndividualCloneMapping mapping : clonedToMappings) {
                locations.add(IndividualCloneInfoResponse.TreeLocation.builder()
                        .treeId(mapping.getClonedTree().getId())
                        .treeName(mapping.getClonedTree().getName())
                        .individualId(mapping.getClonedIndividual().getId())
                        .isCurrentTree(false)
                        .isSourceTree(false)
                        .build());
            }
        }

        // Always add current tree
        locations.add(IndividualCloneInfoResponse.TreeLocation.builder()
                .treeId(currentTreeId)
                .treeName(currentTree.getName())
                .individualId(currentIndividual.getId())
                .isCurrentTree(true)
                .isSourceTree(!isCurrentAClone && !clonedToMappings.isEmpty())
                .build());

        // Sort: current tree first, then source tree, then others
        locations.sort((a, b) -> {
            if (a.isCurrentTree()) return -1;
            if (b.isCurrentTree()) return 1;
            if (a.isSourceTree()) return -1;
            if (b.isSourceTree()) return 1;
            return a.getTreeName().compareTo(b.getTreeName());
        });

        return locations;
    }

    /**
     * Build full name from individual
     */
    private String buildFullName(Individual individual) {
        StringBuilder name = new StringBuilder();
        if (individual.getSurname() != null) {
            name.append(individual.getSurname());
        }
        if (individual.getGivenName() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(individual.getGivenName());
        }
        if (individual.getSuffix() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(individual.getSuffix());
        }
        return name.toString();
    }

    /**
     * Save clone mappings between original and cloned individuals
     */
    private void saveCloneMappings(Map<UUID, Individual> idMapping, FamilyTree sourceTree,
                                   FamilyTree clonedTree, UUID rootIndividualId) {
        List<IndividualCloneMapping> mappings = new ArrayList<>();

        for (Map.Entry<UUID, Individual> entry : idMapping.entrySet()) {
            UUID originalId = entry.getKey();
            Individual clonedIndividual = entry.getValue();

            Individual sourceIndividual = individualRepository.findById(originalId).orElse(null);
            if (sourceIndividual == null) continue;

            IndividualCloneMapping mapping = IndividualCloneMapping.builder()
                    .sourceIndividual(sourceIndividual)
                    .clonedIndividual(clonedIndividual)
                    .sourceTree(sourceTree)
                    .clonedTree(clonedTree)
                    .rootIndividual(originalId.equals(rootIndividualId))
                    .build();

            mappings.add(mapping);
        }

        cloneMappingRepository.saveAll(mappings);
    }

    /**
     * Copy user profile links from source tree to cloned tree
     * If a source individual has a linked user account, link the same user to the cloned individual
     */
    private int copyUserProfiles(Map<UUID, Individual> idMapping, FamilyTree sourceTree, FamilyTree clonedTree) {
        int copiedCount = 0;

        // Get all user profiles for the source tree
        List<UserTreeProfile> sourceProfiles = userTreeProfileRepository.findByTreeId(sourceTree.getId());

        for (UserTreeProfile sourceProfile : sourceProfiles) {
            UUID sourceIndividualId = sourceProfile.getIndividual().getId();

            // Check if this individual was cloned
            Individual clonedIndividual = idMapping.get(sourceIndividualId);
            if (clonedIndividual != null) {
                // Check if user already has a profile in the cloned tree
                Optional<UserTreeProfile> existingProfile = userTreeProfileRepository
                        .findByUserIdAndTreeId(sourceProfile.getUser().getId(), clonedTree.getId());

                if (existingProfile.isEmpty()) {
                    // Create new profile linking the same user to the cloned individual
                    UserTreeProfile newProfile = UserTreeProfile.builder()
                            .user(sourceProfile.getUser())
                            .tree(clonedTree)
                            .individual(clonedIndividual)
                            .build();

                    userTreeProfileRepository.save(newProfile);
                    copiedCount++;
                    log.debug("Copied user profile for user {} to cloned individual {} in tree {}",
                            sourceProfile.getUser().getUsername(), clonedIndividual.getId(), clonedTree.getId());
                }
            }
        }

        return copiedCount;
    }

    /**
     * Collect all related individuals: ancestors, descendants, and their spouses
     */
    private Set<UUID> collectAllRelatedIndividuals(UUID rootIndividualId) {
        Set<UUID> visited = new HashSet<>();
        visited.add(rootIndividualId);

        // Collect ancestors (traverse up)
        collectAncestors(rootIndividualId, visited);

        // Collect descendants (traverse down)
        collectDescendants(rootIndividualId, visited);

        // Collect spouses of root individual
        collectSpouses(rootIndividualId, visited);

        return visited;
    }

    /**
     * Recursively collect all ancestors
     */
    private void collectAncestors(UUID individualId, Set<UUID> visited) {
        List<Relationship> parentRelationships = relationshipRepository.findParents(individualId);

        for (Relationship rel : parentRelationships) {
            UUID parentId = rel.getIndividual1().getId();
            if (!visited.contains(parentId)) {
                visited.add(parentId);
                // Get spouses of this ancestor
                collectSpouses(parentId, visited);
                // Continue up the tree
                collectAncestors(parentId, visited);
            }
        }
    }

    /**
     * Recursively collect all descendants
     */
    private void collectDescendants(UUID individualId, Set<UUID> visited) {
        List<Relationship> childRelationships = relationshipRepository.findChildren(individualId);

        for (Relationship rel : childRelationships) {
            UUID childId = rel.getIndividual2().getId();
            if (!visited.contains(childId)) {
                visited.add(childId);
                // Get spouses of this descendant
                collectSpouses(childId, visited);
                // Continue down the tree
                collectDescendants(childId, visited);
            }
        }
    }

    /**
     * Collect spouses/partners of an individual
     */
    private void collectSpouses(UUID individualId, Set<UUID> visited) {
        List<Relationship> spouseRelationships = relationshipRepository.findSpouses(individualId);

        for (Relationship rel : spouseRelationships) {
            UUID spouseId = rel.getIndividual1().getId().equals(individualId)
                    ? rel.getIndividual2().getId()
                    : rel.getIndividual1().getId();

            if (!visited.contains(spouseId)) {
                visited.add(spouseId);
                // Also get descendants of this spouse (children they have with root's family)
                collectDescendants(spouseId, visited);
            }
        }
    }

    /**
     * Clone individuals to the new tree
     */
    private Map<UUID, Individual> cloneIndividuals(Set<UUID> individualIds, FamilyTree newTree) {
        Map<UUID, Individual> idMapping = new HashMap<>();

        List<Individual> originals = individualRepository.findAllById(individualIds);

        for (Individual original : originals) {
            Individual cloned = Individual.builder()
                    .tree(newTree)
                    .givenName(original.getGivenName())
                    .surname(original.getSurname())
                    .suffix(original.getSuffix())
                    .gender(original.getGender())
                    .birthDate(original.getBirthDate())
                    .birthPlace(original.getBirthPlace())
                    .deathDate(original.getDeathDate())
                    .deathPlace(original.getDeathPlace())
                    .biography(original.getBiography())
                    .notes(original.getNotes())
                    .facebookLink(original.getFacebookLink())
                    .phoneNumber(original.getPhoneNumber())
                    // profilePictureUrl will be updated when cloning media
                    .build();

            cloned = individualRepository.save(cloned);
            idMapping.put(original.getId(), cloned);
        }

        return idMapping;
    }

    /**
     * Clone relationships between cloned individuals
     */
    private List<Relationship> cloneRelationships(
            Set<UUID> originalIndividualIds,
            Map<UUID, Individual> idMapping,
            FamilyTree newTree) {

        List<Relationship> clonedRelationships = new ArrayList<>();

        // Get all relationships where both individuals are in our clone set
        List<Relationship> allRelationships = relationshipRepository.findByTreeId(
                newTree.getSourceTreeId());

        Set<String> processedPairs = new HashSet<>();

        for (Relationship original : allRelationships) {
            UUID ind1Id = original.getIndividual1().getId();
            UUID ind2Id = original.getIndividual2().getId();

            // Only clone if both individuals are in our set
            if (originalIndividualIds.contains(ind1Id) && originalIndividualIds.contains(ind2Id)) {
                // Create a unique key for this relationship to avoid duplicates
                String pairKey = createPairKey(ind1Id, ind2Id, original.getType());
                if (processedPairs.contains(pairKey)) {
                    continue;
                }
                processedPairs.add(pairKey);

                Individual newInd1 = idMapping.get(ind1Id);
                Individual newInd2 = idMapping.get(ind2Id);

                if (newInd1 != null && newInd2 != null) {
                    Relationship cloned = Relationship.builder()
                            .tree(newTree)
                            .individual1(newInd1)
                            .individual2(newInd2)
                            .type(original.getType())
                            .startDate(original.getStartDate())
                            .endDate(original.getEndDate())
                            .build();

                    cloned = relationshipRepository.save(cloned);
                    clonedRelationships.add(cloned);
                }
            }
        }

        return clonedRelationships;
    }

    /**
     * Clone media files from old individuals to new individuals
     */
    private int cloneMedia(Map<UUID, Individual> idMapping, UUID newTreeId) {
        int totalCloned = 0;

        for (Map.Entry<UUID, Individual> entry : idMapping.entrySet()) {
            UUID originalId = entry.getKey();
            Individual newIndividual = entry.getValue();

            // Get all media for original individual
            List<Media> originalMedia = mediaRepository.findByIndividualId(originalId);

            for (Media original : originalMedia) {
                try {
                    // Download original file from MinIO
                    InputStream fileStream = minioService.downloadFile(original.getStoragePath());

                    // Generate new storage path
                    String newPath = minioService.generateObjectName(
                            newTreeId,
                            newIndividual.getId(),
                            original.getFilename()
                    );

                    // Upload to new location
                    minioService.uploadFile(
                            fileStream,
                            newPath,
                            original.getMimeType(),
                            original.getFileSize()
                    );

                    // Clone thumbnail if it exists (for images)
                    if (isImage(original.getMimeType())) {
                        cloneThumbnail(original.getStoragePath(), newPath);
                    }

                    // Create new media record
                    Media clonedMedia = Media.builder()
                            .individual(newIndividual)
                            .type(original.getType())
                            .filename(original.getFilename())
                            .storagePath(newPath)
                            .caption(original.getCaption())
                            .fileSize(original.getFileSize())
                            .mimeType(original.getMimeType())
                            .build();

                    mediaRepository.save(clonedMedia);
                    totalCloned++;

                } catch (Exception e) {
                    log.error("Failed to clone media file {} for individual {}",
                            original.getFilename(), originalId, e);
                    // Continue with other files
                }
            }

            // Clone avatar separately
            cloneAvatar(originalId, newIndividual, newTreeId);
        }

        return totalCloned;
    }

    /**
     * Clone thumbnail for an image
     */
    private void cloneThumbnail(String originalPath, String newPath) {
        try {
            String originalThumbPath = minioService.generateThumbnailName(originalPath);
            if (minioService.fileExists(originalThumbPath)) {
                // Get file metadata FIRST
                StatObjectResponse stat = minioService.getFileMetadata(originalThumbPath);
                long fileSize = stat.size();
                String contentType = stat.contentType();

                // Then download
                InputStream thumbStream = minioService.downloadFile(originalThumbPath);
                String newThumbPath = minioService.generateThumbnailName(newPath);

                // Upload with correct file size
                minioService.uploadFile(thumbStream, newThumbPath, contentType != null ? contentType : "image/jpeg", fileSize);
                log.debug("Cloned thumbnail from {} to {}", originalThumbPath, newThumbPath);
            }
        } catch (Exception e) {
            log.warn("Could not clone thumbnail for {}: {}", originalPath, e.getMessage());
        }
    }

    /**
     * Check if MIME type is an image
     */
    private boolean isImage(String mimeType) {
        if (mimeType == null) return false;
        String lower = mimeType.toLowerCase();
        return lower.startsWith("image/");
    }

    /**
     * Clone avatar image for an individual
     */
    private void cloneAvatar(UUID originalId, Individual newIndividual, UUID newTreeId) {
        try {
            Individual original = individualRepository.findById(originalId).orElse(null);
            if (original == null || original.getProfilePictureUrl() == null) {
                log.debug("No avatar to clone for individual {}", originalId);
                return;
            }

            // Skip if already has avatar (set in some other way)
            if (newIndividual.getProfilePictureUrl() != null) {
                log.debug("Individual {} already has avatar, skipping", newIndividual.getId());
                return;
            }

            // List all files in avatar directory to find the actual avatar file
            String avatarDir = "avatars/individuals/" + originalId + "/";
            log.debug("Looking for avatar files in directory: {}", avatarDir);

            List<String> avatarFiles = minioService.listFiles(avatarDir);
            log.debug("Found {} files in avatar directory for individual {}", avatarFiles.size(), originalId);

            for (String avatarFile : avatarFiles) {
                // Skip if not an avatar file (e.g., might have other files)
                if (!avatarFile.contains("/avatar.") && !avatarFile.contains("/avatar")) {
                    continue;
                }

                log.debug("Found avatar file: {}", avatarFile);

                try {
                    // Get file metadata FIRST (before downloading)
                    StatObjectResponse stat = minioService.getFileMetadata(avatarFile);
                    long fileSize = stat.size();
                    String contentType = stat.contentType();

                    log.debug("Avatar metadata: size={}, type={}", fileSize, contentType);

                    // Download original avatar
                    InputStream avatarStream = minioService.downloadFile(avatarFile);

                    // Extract extension from original file
                    String extension = "";
                    int dotIndex = avatarFile.lastIndexOf('.');
                    if (dotIndex > 0) {
                        extension = avatarFile.substring(dotIndex);
                    }

                    // Create new avatar path (convert to lowercase extension for consistency)
                    String newAvatarPath = "avatars/individuals/" + newIndividual.getId() + "/avatar" + extension.toLowerCase();

                    // Upload to new location
                    minioService.uploadFile(avatarStream, newAvatarPath, contentType, fileSize);

                    // Update new individual with avatar URL
                    String avatarUrl = "/api/trees/" + newTreeId + "/individuals/" +
                            newIndividual.getId() + "/avatar";
                    newIndividual.setProfilePictureUrl(avatarUrl);
                    individualRepository.save(newIndividual);

                    log.info("Cloned avatar for individual {} -> {} at path {}",
                            originalId, newIndividual.getId(), newAvatarPath);
                    return; // Successfully cloned, exit
                } catch (Exception e) {
                    log.warn("Failed to clone avatar file {} for individual {}: {}",
                            avatarFile, originalId, e.getMessage());
                }
            }

            log.debug("No avatar file found for individual {} in directory {}", originalId, avatarDir);
        } catch (Exception e) {
            log.warn("Could not clone avatar for individual {}: {}", originalId, e.getMessage());
        }
    }

    /**
     * Check if user has access to the tree (view permission is enough for cloning)
     */
    private boolean hasAccess(FamilyTree tree, User user) {
        log.debug("Checking access for user {} (id={}, admin={}) to tree {} owned by {} (id={})",
                user.getEmail(), user.getId(), user.isAdmin(),
                tree.getId(), tree.getOwner().getEmail(), tree.getOwner().getId());

        // Admin users have access to all trees
        if (user.isAdmin()) {
            log.debug("User is admin, granting access");
            return true;
        }

        // Owner has access
        if (tree.getOwner().getId().equals(user.getId())) {
            log.debug("User is owner, granting access");
            return true;
        }

        // Check if user has permission through tree permissions
        boolean hasPermission = tree.getPermissions() != null && tree.getPermissions().stream()
                .anyMatch(p -> p.getUser().getId().equals(user.getId()));
        if (hasPermission) {
            log.debug("User has permission, granting access");
            return true;
        }

        // Check if user is linked via UserTreeProfile
        boolean hasProfile = userTreeProfileRepository.existsByUserIdAndTreeId(user.getId(), tree.getId());
        if (hasProfile) {
            log.debug("User has tree profile link, granting access");
            return true;
        }

        // If this is a cloned tree, check if user has access to the source tree
        if (tree.getSourceTreeId() != null) {
            FamilyTree sourceTree = treeRepository.findById(tree.getSourceTreeId()).orElse(null);
            if (sourceTree != null) {
                if (userTreeProfileRepository.existsByUserIdAndTreeId(user.getId(), sourceTree.getId())) {
                    log.debug("User has profile in source tree, granting access");
                    return true;
                }
                if (sourceTree.getOwner().getId().equals(user.getId())) {
                    log.debug("User is owner of source tree, granting access");
                    return true;
                }
                boolean hasSourcePermission = sourceTree.getPermissions() != null && sourceTree.getPermissions().stream()
                        .anyMatch(p -> p.getUser().getId().equals(user.getId()));
                if (hasSourcePermission) {
                    log.debug("User has permission on source tree, granting access");
                    return true;
                }
            }
        }

        log.debug("User has no access to tree");
        return false;
    }

    /**
     * Create a unique key for relationship pair to avoid duplicates
     */
    private String createPairKey(UUID ind1, UUID ind2, RelationshipType type) {
        UUID smaller = ind1.compareTo(ind2) < 0 ? ind1 : ind2;
        UUID larger = ind1.compareTo(ind2) < 0 ? ind2 : ind1;
        return smaller + "_" + larger + "_" + type;
    }

    /**
     * Extract storage path from avatar URL
     */
    private String getAvatarPathFromUrl(String url) {
        if (url == null) return null;
        // URL format: /api/trees/{treeId}/individuals/{indId}/avatar
        // We need to find the corresponding storage path
        return url; // Simplified - avatar handling is done separately
    }
}
