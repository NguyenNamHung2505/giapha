package com.familytree.service;

import com.familytree.dto.relationship.CreateRelationshipRequest;
import com.familytree.dto.relationship.RelationshipResponse;
import com.familytree.dto.relationship.UpdateRelationshipRequest;
import com.familytree.exception.BadRequestException;
import com.familytree.exception.ResourceNotFoundException;
import com.familytree.exception.UnauthorizedException;
import com.familytree.model.*;
import com.familytree.repository.FamilyTreeRepository;
import com.familytree.repository.IndividualRepository;
import com.familytree.repository.RelationshipRepository;
import com.familytree.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing relationships between individuals
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RelationshipService {

    private final RelationshipRepository relationshipRepository;
    private final IndividualRepository individualRepository;
    private final FamilyTreeRepository treeRepository;
    private final UserRepository userRepository;

    /**
     * Create a new relationship between two individuals
     */
    public RelationshipResponse createRelationship(UUID treeId, CreateRelationshipRequest request, String userEmail) {
        log.info("Creating relationship in tree {} for user '{}'", treeId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization - only owner can create relationships
        if (!isOwner(tree, userEmail)) {
            throw new UnauthorizedException("Only the tree owner can create relationships");
        }

        // Fetch both individuals
        Individual individual1 = individualRepository.findById(request.getIndividual1Id())
                .orElseThrow(() -> new ResourceNotFoundException("Individual 1 not found with ID: " + request.getIndividual1Id()));

        Individual individual2 = individualRepository.findById(request.getIndividual2Id())
                .orElseThrow(() -> new ResourceNotFoundException("Individual 2 not found with ID: " + request.getIndividual2Id()));

        // Validate both individuals belong to the same tree
        if (!individual1.getTree().getId().equals(treeId) || !individual2.getTree().getId().equals(treeId)) {
            throw new BadRequestException("Both individuals must belong to the same tree");
        }

        // Validate individuals are not the same person
        if (individual1.getId().equals(individual2.getId())) {
            throw new BadRequestException("Cannot create a relationship between the same individual");
        }

        // Check if relationship already exists
        if (relationshipRepository.existsRelationship(individual1.getId(), individual2.getId(), request.getType())) {
            throw new BadRequestException("This relationship already exists between these individuals");
        }

        // Validate relationship type constraints
        validateRelationshipConstraints(individual1, individual2, request.getType());

        Relationship relationship = Relationship.builder()
                .tree(tree)
                .individual1(individual1)
                .individual2(individual2)
                .type(request.getType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .build();

        Relationship savedRelationship = relationshipRepository.save(relationship);
        log.info("Relationship created with ID: {}", savedRelationship.getId());

        return convertToResponse(savedRelationship);
    }

    /**
     * Get a relationship by ID
     */
    @Transactional(readOnly = true)
    public RelationshipResponse getRelationship(UUID relationshipId, String userEmail) {
        log.info("Fetching relationship {} for user '{}'", relationshipId, userEmail);

        Relationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Relationship not found with ID: " + relationshipId));

        // Check authorization
        if (!hasAccess(relationship.getTree(), userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        return convertToResponse(relationship);
    }

    /**
     * List all relationships in a tree
     */
    @Transactional(readOnly = true)
    public List<RelationshipResponse> listRelationships(UUID treeId, String userEmail) {
        log.info("Listing relationships in tree {} for user '{}'", treeId, userEmail);

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization
        if (!hasAccess(tree, userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        List<Relationship> relationships = relationshipRepository.findByTreeId(treeId);

        return relationships.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * List all relationships for a specific individual
     */
    @Transactional(readOnly = true)
    public List<RelationshipResponse> listRelationshipsForIndividual(UUID individualId, String userEmail) {
        log.info("Listing relationships for individual {} for user '{}'", individualId, userEmail);

        Individual individual = individualRepository.findById(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with ID: " + individualId));

        // Check authorization
        if (!hasAccess(individual.getTree(), userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        List<Relationship> relationships = relationshipRepository.findByIndividual(individualId);

        return relationships.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get parents of an individual
     */
    @Transactional(readOnly = true)
    public List<RelationshipResponse> getParents(UUID individualId, String userEmail) {
        log.info("Fetching parents for individual {} for user '{}'", individualId, userEmail);

        Individual individual = individualRepository.findById(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with ID: " + individualId));

        // Check authorization
        if (!hasAccess(individual.getTree(), userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        List<Relationship> parents = relationshipRepository.findParents(individualId);

        return parents.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get children of an individual
     */
    @Transactional(readOnly = true)
    public List<RelationshipResponse> getChildren(UUID individualId, String userEmail) {
        log.info("Fetching children for individual {} for user '{}'", individualId, userEmail);

        Individual individual = individualRepository.findById(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with ID: " + individualId));

        // Check authorization
        if (!hasAccess(individual.getTree(), userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        List<Relationship> children = relationshipRepository.findChildren(individualId);

        return children.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get spouses/partners of an individual
     */
    @Transactional(readOnly = true)
    public List<RelationshipResponse> getSpouses(UUID individualId, String userEmail) {
        log.info("Fetching spouses for individual {} for user '{}'", individualId, userEmail);

        Individual individual = individualRepository.findById(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with ID: " + individualId));

        // Check authorization
        if (!hasAccess(individual.getTree(), userEmail)) {
            throw new UnauthorizedException("You do not have access to this tree");
        }

        List<Relationship> spouses = relationshipRepository.findSpouses(individualId);

        return spouses.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update a relationship
     */
    public RelationshipResponse updateRelationship(UUID relationshipId, UpdateRelationshipRequest request, String userEmail) {
        log.info("Updating relationship {} for user '{}'", relationshipId, userEmail);

        Relationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Relationship not found with ID: " + relationshipId));

        // Check authorization - only owner can update
        if (!isOwner(relationship.getTree(), userEmail)) {
            throw new UnauthorizedException("Only the tree owner can update relationships");
        }

        relationship.setStartDate(request.getStartDate());
        relationship.setEndDate(request.getEndDate());

        Relationship updatedRelationship = relationshipRepository.save(relationship);
        log.info("Relationship {} updated successfully", relationshipId);

        return convertToResponse(updatedRelationship);
    }

    /**
     * Delete a relationship
     */
    public void deleteRelationship(UUID relationshipId, String userEmail) {
        log.info("Deleting relationship {} for user '{}'", relationshipId, userEmail);

        Relationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Relationship not found with ID: " + relationshipId));

        // Check authorization - only owner can delete
        if (!isOwner(relationship.getTree(), userEmail)) {
            throw new UnauthorizedException("Only the tree owner can delete relationships");
        }

        relationshipRepository.delete(relationship);
        log.info("Relationship {} deleted successfully", relationshipId);
    }

    /**
     * Validate relationship constraints based on type
     */
    private void validateRelationshipConstraints(Individual ind1, Individual ind2, RelationshipType type) {
        switch (type) {
            case PARENT_CHILD:
            case ADOPTED_PARENT_CHILD:
            case STEP_PARENT_CHILD:
                // Check if parent (ind1) is already a descendant of child (ind2)
                // If ind2 is an ancestor of ind1, making ind1 a parent of ind2 would create a circular relationship
                if (isAncestor(ind1.getId(), ind2.getId())) {
                    throw new BadRequestException("This would create a circular relationship");
                }
                break;

            case SIBLING:
                // Siblings should share at least one parent (optional validation)
                // For now, we'll allow any sibling relationship to be created
                break;

            case SPOUSE:
            case PARTNER:
                // No special validation needed for spouse/partner relationships
                break;
        }
    }

    /**
     * Check if potentialAncestor is an ancestor of individual
     * This prevents circular relationships in the family tree
     */
    private boolean isAncestor(UUID individualId, UUID potentialAncestorId) {
        // Get all parents of the individual
        List<Relationship> parents = relationshipRepository.findParents(individualId);

        for (Relationship parentRel : parents) {
            UUID parentId = parentRel.getIndividual1().getId();

            // If we found the potential ancestor as a parent, return true
            if (parentId.equals(potentialAncestorId)) {
                return true;
            }

            // Recursively check if the potential ancestor is an ancestor of this parent
            if (isAncestor(parentId, potentialAncestorId)) {
                return true;
            }
        }

        return false;
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
     * Convert Relationship entity to RelationshipResponse DTO
     */
    private RelationshipResponse convertToResponse(Relationship relationship) {
        return RelationshipResponse.builder()
                .id(relationship.getId())
                .treeId(relationship.getTree().getId())
                .individual1(convertToIndividualSummary(relationship.getIndividual1()))
                .individual2(convertToIndividualSummary(relationship.getIndividual2()))
                .type(relationship.getType())
                .startDate(relationship.getStartDate())
                .endDate(relationship.getEndDate())
                .createdAt(relationship.getCreatedAt())
                .build();
    }

    /**
     * Convert Individual to IndividualSummary for relationship response
     */
    private RelationshipResponse.IndividualSummary convertToIndividualSummary(Individual individual) {
        String fullName = buildFullName(individual);

        return RelationshipResponse.IndividualSummary.builder()
                .id(individual.getId())
                .givenName(individual.getGivenName())
                .surname(individual.getSurname())
                .fullName(fullName)
                .birthDate(individual.getBirthDate())
                .deathDate(individual.getDeathDate())
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
