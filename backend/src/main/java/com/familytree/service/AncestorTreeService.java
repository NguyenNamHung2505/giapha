package com.familytree.service;

import com.familytree.dto.tree.AncestorTreeResponse;
import com.familytree.dto.tree.AncestorTreeResponse.AncestorNode;
import com.familytree.exception.ResourceNotFoundException;
import com.familytree.exception.UnauthorizedException;
import com.familytree.model.FamilyTree;
import com.familytree.model.Individual;
import com.familytree.model.Relationship;
import com.familytree.repository.FamilyTreeRepository;
import com.familytree.repository.IndividualRepository;
import com.familytree.repository.RelationshipRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service for building and fetching ancestor trees
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AncestorTreeService {

    private final IndividualRepository individualRepository;
    private final RelationshipRepository relationshipRepository;
    private final FamilyTreeRepository treeRepository;
    private final com.familytree.repository.UserRepository userRepository;
    private final com.familytree.repository.UserTreeProfileRepository userTreeProfileRepository;

    /**
     * Get ancestor tree for an individual up to specified generations
     *
     * @param individualId  The individual to start from
     * @param generations   Number of generations to fetch (1 = parents, 2 = grandparents, etc.)
     * @param userEmail     The user requesting the data
     * @return AncestorTreeResponse containing the ancestor tree
     */
    public AncestorTreeResponse getAncestorTree(UUID individualId, int generations, String userEmail) {
        log.info("Building ancestor tree for individual {} with {} generations", individualId, generations);

        // Fetch the starting individual
        Individual individual = individualRepository.findByIdWithTree(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with ID: " + individualId));

        // Check authorization
        if (!hasAccess(individual.getTree(), userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        // Build the ancestor tree
        Set<UUID> visited = new HashSet<>();
        int[] totalAncestors = {0};
        int[] maxGeneration = {0};

        AncestorNode rootNode = buildAncestorNode(individual, 0, generations, visited, totalAncestors, maxGeneration);

        return AncestorTreeResponse.builder()
                .root(rootNode)
                .totalAncestors(totalAncestors[0])
                .maxGeneration(maxGeneration[0])
                .build();
    }

    /**
     * Recursively build ancestor node
     */
    private AncestorNode buildAncestorNode(Individual individual, int currentGeneration, int maxGenerations,
                                            Set<UUID> visited, int[] totalAncestors, int[] maxGeneration) {
        if (individual == null || visited.contains(individual.getId())) {
            return null;
        }

        visited.add(individual.getId());

        // Track statistics
        if (currentGeneration > 0) {
            totalAncestors[0]++;
        }
        if (currentGeneration > maxGeneration[0]) {
            maxGeneration[0] = currentGeneration;
        }

        // Build current node
        AncestorNode node = AncestorNode.builder()
                .id(individual.getId())
                .givenName(individual.getGivenName())
                .surname(individual.getSurname())
                .suffix(individual.getSuffix())
                .fullName(buildFullName(individual))
                .gender(individual.getGender() != null ? individual.getGender().name() : null)
                .birthDate(individual.getBirthDate())
                .birthPlace(individual.getBirthPlace())
                .deathDate(individual.getDeathDate())
                .deathPlace(individual.getDeathPlace())
                .profilePictureUrl(individual.getProfilePictureUrl())
                .generation(currentGeneration)
                .parents(new ArrayList<>())
                .build();

        // Stop if we've reached the maximum generations
        if (currentGeneration >= maxGenerations) {
            return node;
        }

        // Find parents
        List<Relationship> parentRelationships = relationshipRepository.findParents(individual.getId());

        for (Relationship parentRel : parentRelationships) {
            Individual parent = parentRel.getIndividual1();
            if (parent != null && !visited.contains(parent.getId())) {
                AncestorNode parentNode = buildAncestorNode(
                        parent,
                        currentGeneration + 1,
                        maxGenerations,
                        visited,
                        totalAncestors,
                        maxGeneration
                );
                if (parentNode != null) {
                    node.getParents().add(parentNode);
                }
            }
        }

        // Sort parents: father first (MALE), then mother (FEMALE)
        node.getParents().sort((a, b) -> {
            if ("MALE".equals(a.getGender()) && !"MALE".equals(b.getGender())) return -1;
            if (!"MALE".equals(a.getGender()) && "MALE".equals(b.getGender())) return 1;
            return 0;
        });

        return node;
    }

    /**
     * Build full name from individual parts
     * Vietnamese name order: Surname (Họ) + Suffix (Tên đệm) + Given Name (Tên)
     */
    private String buildFullName(Individual individual) {
        StringBuilder fullName = new StringBuilder();

        if (individual.getSurname() != null && !individual.getSurname().isEmpty()) {
            fullName.append(individual.getSurname());
        }

        if (individual.getSuffix() != null && !individual.getSuffix().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(individual.getSuffix());
        }

        if (individual.getGivenName() != null && !individual.getGivenName().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(individual.getGivenName());
        }

        return fullName.length() > 0 ? fullName.toString() : "Unknown";
    }

    /**
     * Check if user has access to the tree (owner, has permission, admin, or linked via UserTreeProfile)
     */
    private boolean hasAccess(FamilyTree tree, String userEmail) {
        com.familytree.model.User user = userRepository.findByEmail(userEmail).orElse(null);
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
}
