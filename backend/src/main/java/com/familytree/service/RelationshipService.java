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
import java.util.Set;
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
    private final com.familytree.repository.UserTreeProfileRepository userTreeProfileRepository;

    /**
     * Create a new relationship between two individuals
     */
    public RelationshipResponse createRelationship(UUID treeId, CreateRelationshipRequest request, String userEmail) {
        log.info("Creating relationship in tree {} for user '{}'", treeId, userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Tree not found with ID: " + treeId));

        // Check authorization - owner or admin can create relationships
        if (!canModify(tree, userEmail)) {
            throw new UnauthorizedException("Only the tree owner or admin can create relationships");
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

        // Auto-add sibling relationships when a parent-child relationship is created
        if (isParentChildType(request.getType())) {
            autoAddSiblingRelationships(tree, individual1, individual2);

            // Auto-add parent-child relationship with spouse if exists
            autoAddSpouseParentRelationship(tree, individual1, individual2, request.getType());
        }

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

        // Check authorization - owner or admin can update
        if (!canModify(relationship.getTree(), userEmail)) {
            throw new UnauthorizedException("Only the tree owner or admin can update relationships");
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

        // Check authorization - owner or admin can delete
        if (!canModify(relationship.getTree(), userEmail)) {
            throw new UnauthorizedException("Only the tree owner or admin can delete relationships");
        }

        // Store info before deleting for sibling cleanup
        FamilyTree tree = relationship.getTree();
        Individual parent = relationship.getIndividual1();
        Individual child = relationship.getIndividual2();
        RelationshipType type = relationship.getType();

        relationshipRepository.delete(relationship);
        log.info("Relationship {} deleted successfully", relationshipId);

        // Auto-remove sibling relationships when a parent-child relationship is deleted
        if (isParentChildType(type)) {
            autoRemoveSiblingRelationships(tree, parent, child);
        }
    }

    /**
     * Validate relationship constraints based on type
     */
    private void validateRelationshipConstraints(Individual ind1, Individual ind2, RelationshipType type) {
        switch (type) {
            case PARENT_CHILD:
                // Check if parent (ind1) is already a descendant of child (ind2)
                if (isAncestor(ind1.getId(), ind2.getId())) {
                    throw new BadRequestException("This would create a circular relationship");
                }
                break;

            case FATHER_CHILD:
                // Check if parent (ind1) is already a descendant of child (ind2)
                if (isAncestor(ind1.getId(), ind2.getId())) {
                    throw new BadRequestException("This would create a circular relationship");
                }
                // Check if child already has a biological father
                if (hasExistingBiologicalFather(ind2.getId())) {
                    throw new BadRequestException("Người này đã có cha ruột. Một người không thể có 2 cha ruột.");
                }
                break;

            case MOTHER_CHILD:
                // Check if parent (ind1) is already a descendant of child (ind2)
                if (isAncestor(ind1.getId(), ind2.getId())) {
                    throw new BadRequestException("This would create a circular relationship");
                }
                // Check if child already has a biological mother
                if (hasExistingBiologicalMother(ind2.getId())) {
                    throw new BadRequestException("Người này đã có mẹ ruột. Một người không thể có 2 mẹ ruột.");
                }
                break;

            case ADOPTED_PARENT_CHILD:
            case STEP_PARENT_CHILD:
                // Check if parent (ind1) is already a descendant of child (ind2)
                if (isAncestor(ind1.getId(), ind2.getId())) {
                    throw new BadRequestException("This would create a circular relationship");
                }
                // No restriction on number of adopted/step parents
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
     * Check if an individual already has a biological father (FATHER_CHILD relationship)
     */
    private boolean hasExistingBiologicalFather(UUID childId) {
        List<Relationship> parents = relationshipRepository.findParents(childId);
        return parents.stream()
                .anyMatch(rel -> rel.getType() == RelationshipType.FATHER_CHILD);
    }

    /**
     * Check if an individual already has a biological mother (MOTHER_CHILD relationship)
     */
    private boolean hasExistingBiologicalMother(UUID childId) {
        List<Relationship> parents = relationshipRepository.findParents(childId);
        return parents.stream()
                .anyMatch(rel -> rel.getType() == RelationshipType.MOTHER_CHILD);
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
     * Check if user is the owner of the tree
     */
    private boolean isOwner(FamilyTree tree, String userEmail) {
        return tree.getOwner().getEmail().equals(userEmail);
    }

    /**
     * Check if user can modify tree content (owner or admin)
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
     * Vietnamese name order: Surname (Họ) + Suffix (Tên đệm) + Given Name (Tên)
     * Example: Nguyễn Nam Hưng (Họ: Nguyễn, Tên đệm: Nam, Tên: Hưng)
     */
    private String buildFullName(Individual individual) {
        StringBuilder fullName = new StringBuilder();

        // 1. Surname (Họ) - comes first in Vietnamese names
        if (individual.getSurname() != null && !individual.getSurname().isEmpty()) {
            fullName.append(individual.getSurname());
        }

        // 2. Suffix (Tên đệm) - middle name in Vietnamese
        if (individual.getSuffix() != null && !individual.getSuffix().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(individual.getSuffix());
        }

        // 3. Given Name (Tên) - comes last in Vietnamese names
        if (individual.getGivenName() != null && !individual.getGivenName().isEmpty()) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(individual.getGivenName());
        }

        return fullName.length() > 0 ? fullName.toString() : "Unknown";
    }

    // ==================== Sibling Auto-Management ====================

    /**
     * Check if relationship type is a parent-child type
     */
    private boolean isParentChildType(RelationshipType type) {
        return type == RelationshipType.PARENT_CHILD ||
               type == RelationshipType.MOTHER_CHILD ||
               type == RelationshipType.FATHER_CHILD ||
               type == RelationshipType.ADOPTED_PARENT_CHILD ||
               type == RelationshipType.STEP_PARENT_CHILD;
    }

    /**
     * Auto-add sibling relationships when a parent-child relationship is created.
     * This creates SIBLING relationships between the new child and:
     * 1. All existing children of the same parent
     * 2. All existing children of the parent's spouse(s) (if they are married)
     *
     * @param tree The family tree
     * @param parent The parent individual
     * @param newChild The newly added child
     */
    private void autoAddSiblingRelationships(FamilyTree tree, Individual parent, Individual newChild) {
        // Collect all children that should become siblings
        Set<UUID> potentialSiblingIds = new java.util.HashSet<>();

        // 1. Find all existing children of this parent
        List<Relationship> parentChildRelationships = relationshipRepository.findChildren(parent.getId());
        for (Relationship childRel : parentChildRelationships) {
            potentialSiblingIds.add(childRel.getIndividual2().getId());
        }

        // 2. Find all children of the parent's spouse(s)
        List<Relationship> spouseRelationships = relationshipRepository.findSpouses(parent.getId());
        for (Relationship spouseRel : spouseRelationships) {
            Individual spouse = spouseRel.getIndividual1().getId().equals(parent.getId())
                    ? spouseRel.getIndividual2()
                    : spouseRel.getIndividual1();

            // Get all children of this spouse
            List<Relationship> spouseChildRelationships = relationshipRepository.findChildren(spouse.getId());
            for (Relationship childRel : spouseChildRelationships) {
                potentialSiblingIds.add(childRel.getIndividual2().getId());
            }
        }

        // Remove the new child from the set (can't be sibling of itself)
        potentialSiblingIds.remove(newChild.getId());

        // Create sibling relationships
        for (UUID siblingId : potentialSiblingIds) {
            // Check if sibling relationship already exists
            boolean siblingExists = relationshipRepository.existsRelationship(
                    newChild.getId(), siblingId, RelationshipType.SIBLING);

            if (!siblingExists) {
                Individual sibling = individualRepository.findById(siblingId).orElse(null);
                if (sibling != null) {
                    // Create sibling relationship
                    Relationship siblingRelationship = Relationship.builder()
                            .tree(tree)
                            .individual1(newChild)
                            .individual2(sibling)
                            .type(RelationshipType.SIBLING)
                            .build();

                    relationshipRepository.save(siblingRelationship);
                    log.info("Auto-created sibling relationship between {} and {}",
                            newChild.getId(), siblingId);
                }
            }
        }
    }

    /**
     * Auto-remove sibling relationships when a parent-child relationship is deleted.
     * Only removes sibling relationships if the siblings no longer share ANY common parent.
     *
     * @param tree The family tree
     * @param formerParent The parent whose relationship was removed
     * @param child The child whose parent relationship was removed
     */
    private void autoRemoveSiblingRelationships(FamilyTree tree, Individual formerParent, Individual child) {
        // Find all sibling relationships of this child
        List<Relationship> siblingRelationships = relationshipRepository.findByIndividual(child.getId())
                .stream()
                .filter(rel -> rel.getType() == RelationshipType.SIBLING)
                .collect(Collectors.toList());

        for (Relationship siblingRel : siblingRelationships) {
            // Get the sibling
            Individual sibling = siblingRel.getIndividual1().getId().equals(child.getId())
                    ? siblingRel.getIndividual2()
                    : siblingRel.getIndividual1();

            // Check if they still share a common parent
            boolean stillShareParent = shareCommonParent(child.getId(), sibling.getId());

            if (!stillShareParent) {
                // Remove the sibling relationship
                relationshipRepository.delete(siblingRel);
                log.info("Auto-removed sibling relationship between {} and {} (no common parent)",
                        child.getId(), sibling.getId());
            }
        }
    }

    /**
     * Check if two individuals share at least one common parent or have parents who are spouses.
     * This considers:
     * 1. Direct common parent (same person is parent of both)
     * 2. Parents who are married/partners (parents are spouses)
     */
    private boolean shareCommonParent(UUID individual1Id, UUID individual2Id) {
        // Get parents of individual 1
        List<Relationship> parents1 = relationshipRepository.findParents(individual1Id);
        Set<UUID> parent1Ids = parents1.stream()
                .map(rel -> rel.getIndividual1().getId())
                .collect(Collectors.toSet());

        // Get parents of individual 2
        List<Relationship> parents2 = relationshipRepository.findParents(individual2Id);
        Set<UUID> parent2Ids = parents2.stream()
                .map(rel -> rel.getIndividual1().getId())
                .collect(Collectors.toSet());

        // Check if any parent of individual 2 is also a parent of individual 1 (direct common parent)
        for (UUID parent2Id : parent2Ids) {
            if (parent1Ids.contains(parent2Id)) {
                return true;
            }
        }

        // Check if any parent of individual 1 is a spouse of any parent of individual 2
        for (UUID parent1Id : parent1Ids) {
            List<Relationship> spouseRelationships = relationshipRepository.findSpouses(parent1Id);
            for (Relationship spouseRel : spouseRelationships) {
                UUID spouseId = spouseRel.getIndividual1().getId().equals(parent1Id)
                        ? spouseRel.getIndividual2().getId()
                        : spouseRel.getIndividual1().getId();

                if (parent2Ids.contains(spouseId)) {
                    return true;
                }
            }
        }

        return false;
    }

    // ==================== Auto-Add Spouse Parent Relationship ====================

    /**
     * Auto-add parent-child relationship with spouse when a child is added.
     * If A and B are spouses (with no other relationship between them),
     * when C is declared as child of A, C should also automatically be a child of B.
     *
     * @param tree The family tree
     * @param parent The parent individual (A)
     * @param child The child individual (C)
     * @param relationshipType The type of parent-child relationship
     */
    private void autoAddSpouseParentRelationship(FamilyTree tree, Individual parent, Individual child, RelationshipType relationshipType) {
        // Find all spouses of the parent
        List<Relationship> spouseRelationships = relationshipRepository.findSpouses(parent.getId());

        for (Relationship spouseRel : spouseRelationships) {
            // Get the spouse individual
            Individual spouse = spouseRel.getIndividual1().getId().equals(parent.getId())
                    ? spouseRel.getIndividual2()
                    : spouseRel.getIndividual1();

            // Check if the spouse and parent ONLY have a spouse/partner relationship (no other relationships)
            if (hasOnlySpouseRelationship(parent.getId(), spouse.getId())) {
                // Check if the parent-child relationship already exists between spouse and child
                boolean relationshipExists = relationshipRepository.existsRelationship(
                        spouse.getId(), child.getId(), relationshipType);

                // Also check for any other parent-child type relationship
                boolean anyParentChildExists = hasAnyParentChildRelationship(spouse.getId(), child.getId());

                if (!relationshipExists && !anyParentChildExists) {
                    // Create the parent-child relationship between spouse and child
                    Relationship newRelationship = Relationship.builder()
                            .tree(tree)
                            .individual1(spouse)
                            .individual2(child)
                            .type(relationshipType)
                            .build();

                    relationshipRepository.save(newRelationship);
                    log.info("Auto-created {} relationship between spouse {} and child {}",
                            relationshipType, spouse.getId(), child.getId());

                    // Also auto-add sibling relationships for this new parent-child relationship
                    autoAddSiblingRelationships(tree, spouse, child);
                }
            }
        }
    }

    /**
     * Check if two individuals have ONLY a spouse/partner relationship (no other relationships)
     */
    private boolean hasOnlySpouseRelationship(UUID individual1Id, UUID individual2Id) {
        // Get all relationships between these two individuals
        List<Relationship> relationships = relationshipRepository.findByIndividual(individual1Id)
                .stream()
                .filter(rel -> rel.getIndividual1().getId().equals(individual2Id) ||
                              rel.getIndividual2().getId().equals(individual2Id))
                .collect(Collectors.toList());

        // Check that all relationships are spouse or partner type
        if (relationships.isEmpty()) {
            return false;
        }

        for (Relationship rel : relationships) {
            if (rel.getType() != RelationshipType.SPOUSE && rel.getType() != RelationshipType.PARTNER) {
                return false;
            }
        }

        return true;
    }

    /**
     * Check if any parent-child relationship exists between two individuals
     */
    private boolean hasAnyParentChildRelationship(UUID parentId, UUID childId) {
        return relationshipRepository.existsRelationship(parentId, childId, RelationshipType.PARENT_CHILD) ||
               relationshipRepository.existsRelationship(parentId, childId, RelationshipType.MOTHER_CHILD) ||
               relationshipRepository.existsRelationship(parentId, childId, RelationshipType.FATHER_CHILD) ||
               relationshipRepository.existsRelationship(parentId, childId, RelationshipType.ADOPTED_PARENT_CHILD) ||
               relationshipRepository.existsRelationship(parentId, childId, RelationshipType.STEP_PARENT_CHILD);
    }
}
