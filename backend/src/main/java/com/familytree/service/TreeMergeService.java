package com.familytree.service;

import com.familytree.dto.merge.*;
import com.familytree.exception.BadRequestException;
import com.familytree.exception.ResourceNotFoundException;
import com.familytree.exception.UnauthorizedException;
import com.familytree.model.*;
import com.familytree.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for merging family trees (like Git merge)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TreeMergeService {

    private final FamilyTreeRepository treeRepository;
    private final IndividualRepository individualRepository;
    private final RelationshipRepository relationshipRepository;
    private final UserRepository userRepository;
    private final IndividualCloneMappingRepository cloneMappingRepository;
    private final TreePermissionRepository permissionRepository;

    // Matching score thresholds
    private static final int EXACT_NAME_SCORE = 100;
    private static final int FUZZY_NAME_SCORE = 50;
    private static final int EXACT_BIRTH_DATE_SCORE = 80;
    private static final int CLOSE_BIRTH_DATE_SCORE = 40;
    private static final int BIRTH_PLACE_SCORE = 30;
    private static final int MATCH_THRESHOLD = 100;

    /**
     * Preview a merge operation without making changes
     */
    @Transactional(readOnly = true)
    public MergePreviewResponse previewMerge(UUID targetTreeId, TreeMergeRequest request, String userEmail) {
        log.info("Previewing merge from tree {} to tree {} by user {}", 
                request.getSourceTreeId(), targetTreeId, userEmail);

        // 1. Validate the merge request
        ValidationResult validation = validateMerge(targetTreeId, request, userEmail);
        
        if (!validation.isValid()) {
            return buildErrorResponse(targetTreeId, request, validation);
        }

        FamilyTree targetTree = validation.getTargetTree();
        FamilyTree sourceTree = validation.getSourceTree();

        // 2. Find matching individuals between trees
        List<IndividualMatch> matches = findMatchingIndividuals(sourceTree, targetTree);
        
        // 3. Identify new individuals (in source but not matched to target)
        List<MergePreviewResponse.IndividualInfo> newIndividuals = 
                findNewIndividuals(sourceTree, matches, request);
        
        // 4. Identify updates (matched individuals with different data)
        List<MergePreviewResponse.IndividualInfo> updatedIndividuals = 
                findUpdatedIndividuals(matches);
        
        // 5. Find new relationships
        List<MergePreviewResponse.RelationshipInfo> newRelationships = 
                findNewRelationships(sourceTree, targetTree, matches);
        
        // 6. Detect conflicts
        List<MergeConflict> conflicts = detectConflicts(matches, sourceTree, targetTree);
        
        // 7. Validate data integrity
        List<MergePreviewResponse.ValidationError> dataErrors = 
                validateDataIntegrity(newIndividuals, newRelationships);
        
        // 8. Build detailed individual previews for interactive selection
        List<MergePreviewResponse.IndividualPreview> individualPreviews = 
                buildIndividualPreviews(sourceTree, targetTree, matches, conflicts);
        
        // 9. Build response
        return buildPreviewResponse(targetTreeId, request, matches, newIndividuals, 
                updatedIndividuals, newRelationships, conflicts, dataErrors, 
                validation.getWarnings(), individualPreviews);
    }

    /**
     * Execute the merge operation
     */
    @Transactional
    public MergeResultResponse executeMerge(UUID targetTreeId, TreeMergeRequest request, String userEmail) {
        log.info("Executing merge from tree {} to tree {} by user {}", 
                request.getSourceTreeId(), targetTreeId, userEmail);

        // 1. Validate
        ValidationResult validation = validateMerge(targetTreeId, request, userEmail);
        if (!validation.isValid()) {
            throw new BadRequestException("Merge validation failed: " + 
                    validation.getErrors().stream()
                            .map(MergePreviewResponse.ValidationError::getMessage)
                            .collect(Collectors.joining(", ")));
        }

        FamilyTree targetTree = validation.getTargetTree();
        FamilyTree sourceTree = validation.getSourceTree();

        // 2. Find matches
        List<IndividualMatch> matches = findMatchingIndividuals(sourceTree, targetTree);
        
        // 3. Detect conflicts and check they're resolved
        List<MergeConflict> conflicts = detectConflicts(matches, sourceTree, targetTree);
        if (!conflicts.isEmpty() && request.getConflictResolution() == ConflictResolution.MANUAL) {
            if (request.getManualResolutions() == null || request.getManualResolutions().isEmpty()) {
                throw new BadRequestException("Manual conflict resolution required but no resolutions provided");
            }
        }

        // 4. Execute merge
        int individualsAdded = 0;
        int individualsUpdated = 0;
        int relationshipsAdded = 0;
        int conflictsResolved = conflicts.size();

        // Create ID mapping for new individuals
        Map<UUID, Individual> sourceToTargetMapping = new HashMap<>();
        
        // Map existing matches
        for (IndividualMatch match : matches) {
            Individual targetInd = individualRepository.findById(match.getTargetIndividualId()).orElse(null);
            if (targetInd != null) {
                sourceToTargetMapping.put(match.getSourceIndividualId(), targetInd);
            }
        }

        // Add new individuals
        List<Individual> sourceIndividuals = individualRepository.findByTreeId(sourceTree.getId());
        Set<UUID> matchedSourceIds = matches.stream()
                .map(IndividualMatch::getSourceIndividualId)
                .collect(Collectors.toSet());
        
        for (Individual sourceInd : sourceIndividuals) {
            if (!matchedSourceIds.contains(sourceInd.getId())) {
                // Check if should be included
                if (request.getSelectedIndividualIds() != null && 
                        !request.getSelectedIndividualIds().isEmpty() &&
                        !request.getSelectedIndividualIds().contains(sourceInd.getId())) {
                    continue;
                }
                
                // Clone individual to target tree
                Individual newInd = cloneIndividual(sourceInd, targetTree);
                newInd = individualRepository.save(newInd);
                sourceToTargetMapping.put(sourceInd.getId(), newInd);
                individualsAdded++;
            }
        }

        // Update matched individuals based on conflict resolution
        if (request.getConflictResolution() == ConflictResolution.THEIRS || 
                request.getConflictResolution() == ConflictResolution.AUTO_MERGE) {
            for (IndividualMatch match : matches) {
                if (updateFromSource(match, request.getConflictResolution())) {
                    individualsUpdated++;
                }
            }
        }

        // Add relationships
        List<Relationship> sourceRelationships = relationshipRepository.findByTreeId(sourceTree.getId());
        for (Relationship sourceRel : sourceRelationships) {
            Individual targetInd1 = sourceToTargetMapping.get(sourceRel.getIndividual1().getId());
            Individual targetInd2 = sourceToTargetMapping.get(sourceRel.getIndividual2().getId());
            
            if (targetInd1 != null && targetInd2 != null) {
                // Check if relationship already exists
                boolean exists = relationshipRepository.existsByTreeIdAndIndividualsAndType(
                        targetTree.getId(), targetInd1.getId(), targetInd2.getId(), sourceRel.getType());
                
                if (!exists) {
                    Relationship newRel = Relationship.builder()
                            .tree(targetTree)
                            .individual1(targetInd1)
                            .individual2(targetInd2)
                            .type(sourceRel.getType())
                            .startDate(sourceRel.getStartDate())
                            .endDate(sourceRel.getEndDate())
                            .build();
                    relationshipRepository.save(newRel);
                    relationshipsAdded++;
                }
            }
        }

        log.info("Merge completed: {} individuals added, {} updated, {} relationships added",
                individualsAdded, individualsUpdated, relationshipsAdded);

        return MergeResultResponse.builder()
                .success(true)
                .targetTreeId(targetTreeId)
                .sourceTreeId(request.getSourceTreeId())
                .mergeId(UUID.randomUUID())
                .summary(MergeResultResponse.MergeSummary.builder()
                        .individualsAdded(individualsAdded)
                        .individualsUpdated(individualsUpdated)
                        .relationshipsAdded(relationshipsAdded)
                        .conflictsResolved(conflictsResolved)
                        .build())
                .message(String.format("Successfully merged %d individuals and %d relationships", 
                        individualsAdded + individualsUpdated, relationshipsAdded))
                .mergedAt(LocalDateTime.now())
                .canUndo(true)
                .build();
    }

    /**
     * Validate merge request
     */
    private ValidationResult validateMerge(UUID targetTreeId, TreeMergeRequest request, String userEmail) {
        List<MergePreviewResponse.ValidationError> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Get user
        User user = userRepository.findByEmail(userEmail)
                .orElseGet(() -> userRepository.findByUsername(userEmail).orElse(null));
        
        if (user == null) {
            errors.add(MergePreviewResponse.ValidationError.builder()
                    .code("USER_NOT_FOUND")
                    .message("User not found")
                    .blocking(true)
                    .build());
            return new ValidationResult(false, errors, warnings, null, null, null);
        }

        // Get target tree
        FamilyTree targetTree = treeRepository.findById(targetTreeId).orElse(null);
        if (targetTree == null) {
            errors.add(MergePreviewResponse.ValidationError.builder()
                    .code("TARGET_TREE_NOT_FOUND")
                    .message("Target tree not found")
                    .blocking(true)
                    .build());
            return new ValidationResult(false, errors, warnings, null, null, user);
        }

        // Get source tree
        FamilyTree sourceTree = treeRepository.findById(request.getSourceTreeId()).orElse(null);
        if (sourceTree == null) {
            errors.add(MergePreviewResponse.ValidationError.builder()
                    .code("SOURCE_TREE_NOT_FOUND")
                    .message("Source tree not found")
                    .blocking(true)
                    .build());
            return new ValidationResult(false, errors, warnings, targetTree, null, user);
        }

        // Self-merge check
        if (targetTreeId.equals(request.getSourceTreeId())) {
            errors.add(MergePreviewResponse.ValidationError.builder()
                    .code("SELF_MERGE")
                    .message("Cannot merge a tree into itself")
                    .blocking(true)
                    .build());
        }

        // Permission checks
        if (!hasEditPermission(targetTree, user)) {
            errors.add(MergePreviewResponse.ValidationError.builder()
                    .code("NO_EDIT_PERMISSION")
                    .message("You don't have permission to modify the target tree")
                    .blocking(true)
                    .build());
        }

        if (!hasViewPermission(sourceTree, user)) {
            errors.add(MergePreviewResponse.ValidationError.builder()
                    .code("NO_VIEW_PERMISSION")
                    .message("You don't have access to the source tree")
                    .blocking(true)
                    .build());
        }

        // Circular relationship check
        if (wouldCreateCircularRelationship(targetTree, sourceTree)) {
            errors.add(MergePreviewResponse.ValidationError.builder()
                    .code("CIRCULAR_RELATIONSHIP")
                    .message("Cannot merge: would create circular relationship")
                    .blocking(true)
                    .build());
        }

        // Warnings
        int sourceCount = (int) individualRepository.countByTreeId(sourceTree.getId());
        int targetCount = (int) individualRepository.countByTreeId(targetTree.getId());
        
        if (sourceCount > 100) {
            warnings.add("Source tree has " + sourceCount + " individuals. Merge may take some time.");
        }
        
        if (sourceCount > targetCount * 2) {
            warnings.add("Source tree is much larger than target. Consider using IMPORT strategy to select specific individuals.");
        }

        return new ValidationResult(
                errors.stream().noneMatch(MergePreviewResponse.ValidationError::isBlocking),
                errors, warnings, targetTree, sourceTree, user);
    }

    /**
     * Find matching individuals between source and target trees
     */
    private List<IndividualMatch> findMatchingIndividuals(FamilyTree sourceTree, FamilyTree targetTree) {
        List<IndividualMatch> matches = new ArrayList<>();
        
        // First, check clone mappings for exact matches
        List<IndividualCloneMapping> cloneMappings = cloneMappingRepository
                .findBySourceTreeIdAndClonedTreeId(sourceTree.getId(), targetTree.getId());
        
        // Also check reverse (if target was cloned from source)
        List<IndividualCloneMapping> reverseMappings = cloneMappingRepository
                .findBySourceTreeIdAndClonedTreeId(targetTree.getId(), sourceTree.getId());

        Set<UUID> matchedSourceIds = new HashSet<>();
        Set<UUID> matchedTargetIds = new HashSet<>();

        // Add clone mapping matches
        for (IndividualCloneMapping mapping : cloneMappings) {
            matches.add(IndividualMatch.builder()
                    .sourceIndividualId(mapping.getSourceIndividual().getId())
                    .targetIndividualId(mapping.getClonedIndividual().getId())
                    .sourceName(buildFullName(mapping.getSourceIndividual()))
                    .targetName(buildFullName(mapping.getClonedIndividual()))
                    .matchScore(100)
                    .matchType(IndividualMatch.MatchType.CLONE_MAPPING)
                    .matchReason("Clone mapping exists")
                    .clonedMatch(true)
                    .build());
            matchedSourceIds.add(mapping.getSourceIndividual().getId());
            matchedTargetIds.add(mapping.getClonedIndividual().getId());
        }

        // Add reverse mappings
        for (IndividualCloneMapping mapping : reverseMappings) {
            matches.add(IndividualMatch.builder()
                    .sourceIndividualId(mapping.getClonedIndividual().getId())
                    .targetIndividualId(mapping.getSourceIndividual().getId())
                    .sourceName(buildFullName(mapping.getClonedIndividual()))
                    .targetName(buildFullName(mapping.getSourceIndividual()))
                    .matchScore(100)
                    .matchType(IndividualMatch.MatchType.CLONE_MAPPING)
                    .matchReason("Reverse clone mapping exists")
                    .clonedMatch(true)
                    .build());
            matchedSourceIds.add(mapping.getClonedIndividual().getId());
            matchedTargetIds.add(mapping.getSourceIndividual().getId());
        }

        // For unmatched individuals, try fuzzy matching
        List<Individual> sourceIndividuals = individualRepository.findByTreeId(sourceTree.getId());
        List<Individual> targetIndividuals = individualRepository.findByTreeId(targetTree.getId());

        for (Individual source : sourceIndividuals) {
            if (matchedSourceIds.contains(source.getId())) continue;

            IndividualMatch bestMatch = null;
            int bestScore = 0;

            for (Individual target : targetIndividuals) {
                if (matchedTargetIds.contains(target.getId())) continue;

                int score = calculateMatchScore(source, target);
                if (score >= MATCH_THRESHOLD && score > bestScore) {
                    bestScore = score;
                    bestMatch = IndividualMatch.builder()
                            .sourceIndividualId(source.getId())
                            .targetIndividualId(target.getId())
                            .sourceName(buildFullName(source))
                            .targetName(buildFullName(target))
                            .matchScore(score)
                            .matchType(getMatchType(score))
                            .matchReason(getMatchReason(source, target))
                            .clonedMatch(false)
                            .build();
                }
            }

            if (bestMatch != null) {
                matches.add(bestMatch);
                matchedSourceIds.add(source.getId());
                matchedTargetIds.add(bestMatch.getTargetIndividualId());
            }
        }

        return matches;
    }

    /**
     * Calculate match score between two individuals
     */
    private int calculateMatchScore(Individual source, Individual target) {
        int score = 0;

        // Name matching
        String sourceName = buildFullName(source).toLowerCase().trim();
        String targetName = buildFullName(target).toLowerCase().trim();
        
        if (sourceName.equals(targetName)) {
            score += EXACT_NAME_SCORE;
        } else if (fuzzyNameMatch(sourceName, targetName)) {
            score += FUZZY_NAME_SCORE;
        }

        // Birth date matching
        if (source.getBirthDate() != null && target.getBirthDate() != null) {
            if (source.getBirthDate().equals(target.getBirthDate())) {
                score += EXACT_BIRTH_DATE_SCORE;
            } else if (Math.abs(source.getBirthDate().getYear() - target.getBirthDate().getYear()) <= 1) {
                score += CLOSE_BIRTH_DATE_SCORE;
            }
        }

        // Birth place matching
        if (source.getBirthPlace() != null && target.getBirthPlace() != null &&
                source.getBirthPlace().equalsIgnoreCase(target.getBirthPlace())) {
            score += BIRTH_PLACE_SCORE;
        }

        // Gender must match if specified
        if (source.getGender() != null && target.getGender() != null &&
                !source.getGender().equals(target.getGender())) {
            score = 0; // Gender mismatch = no match
        }

        return score;
    }

    /**
     * Simple fuzzy name matching
     */
    private boolean fuzzyNameMatch(String name1, String name2) {
        // Check if names are similar (e.g., one contains the other, or Levenshtein distance is small)
        if (name1.contains(name2) || name2.contains(name1)) {
            return true;
        }
        
        // Simple similarity check
        String[] parts1 = name1.split("\\s+");
        String[] parts2 = name2.split("\\s+");
        
        int matches = 0;
        for (String p1 : parts1) {
            for (String p2 : parts2) {
                if (p1.equals(p2)) matches++;
            }
        }
        
        return matches >= Math.min(parts1.length, parts2.length) / 2;
    }

    /**
     * Detect conflicts between matched individuals
     */
    private List<MergeConflict> detectConflicts(List<IndividualMatch> matches, 
            FamilyTree sourceTree, FamilyTree targetTree) {
        List<MergeConflict> conflicts = new ArrayList<>();

        for (IndividualMatch match : matches) {
            Individual source = individualRepository.findById(match.getSourceIndividualId()).orElse(null);
            Individual target = individualRepository.findById(match.getTargetIndividualId()).orElse(null);
            
            if (source == null || target == null) continue;

            // Check each field for conflicts
            checkFieldConflict(conflicts, match, "givenName", 
                    source.getGivenName(), target.getGivenName());
            checkFieldConflict(conflicts, match, "surname", 
                    source.getSurname(), target.getSurname());
            checkFieldConflict(conflicts, match, "birthDate", 
                    source.getBirthDate() != null ? source.getBirthDate().toString() : null,
                    target.getBirthDate() != null ? target.getBirthDate().toString() : null);
            checkFieldConflict(conflicts, match, "deathDate", 
                    source.getDeathDate() != null ? source.getDeathDate().toString() : null,
                    target.getDeathDate() != null ? target.getDeathDate().toString() : null);
            checkFieldConflict(conflicts, match, "birthPlace", 
                    source.getBirthPlace(), target.getBirthPlace());
            checkFieldConflict(conflicts, match, "deathPlace", 
                    source.getDeathPlace(), target.getDeathPlace());
        }

        return conflicts;
    }

    private void checkFieldConflict(List<MergeConflict> conflicts, IndividualMatch match,
            String field, String sourceValue, String targetValue) {
        // Ignore if both null or both equal
        if (Objects.equals(sourceValue, targetValue)) return;
        
        // Only conflict if both have values and they differ
        if (sourceValue != null && targetValue != null && !sourceValue.equals(targetValue)) {
            conflicts.add(MergeConflict.builder()
                    .conflictId(UUID.randomUUID())
                    .targetIndividualId(match.getTargetIndividualId())
                    .sourceIndividualId(match.getSourceIndividualId())
                    .individualName(match.getTargetName())
                    .conflictType(MergeConflict.ConflictType.DATA_MISMATCH)
                    .field(field)
                    .targetValue(targetValue)
                    .sourceValue(sourceValue)
                    .severity(MergeConflict.ConflictSeverity.WARNING)
                    .suggestion("Choose which value to keep")
                    .build());
        }
    }

    // Helper methods
    private List<MergePreviewResponse.IndividualInfo> findNewIndividuals(FamilyTree sourceTree, 
            List<IndividualMatch> matches, TreeMergeRequest request) {
        Set<UUID> matchedSourceIds = matches.stream()
                .map(IndividualMatch::getSourceIndividualId)
                .collect(Collectors.toSet());
        
        return individualRepository.findByTreeId(sourceTree.getId()).stream()
                .filter(ind -> !matchedSourceIds.contains(ind.getId()))
                .filter(ind -> request.getSelectedIndividualIds() == null || 
                        request.getSelectedIndividualIds().isEmpty() ||
                        request.getSelectedIndividualIds().contains(ind.getId()))
                .map(this::toIndividualInfo)
                .collect(Collectors.toList());
    }

    private List<MergePreviewResponse.IndividualInfo> findUpdatedIndividuals(List<IndividualMatch> matches) {
        return matches.stream()
                .filter(m -> !m.isClonedMatch() || m.getMatchScore() < 100)
                .map(match -> {
                    Individual target = individualRepository.findById(match.getTargetIndividualId()).orElse(null);
                    if (target == null) return null;
                    return MergePreviewResponse.IndividualInfo.builder()
                            .id(target.getId())
                            .name(buildFullName(target))
                            .birthDate(target.getBirthDate() != null ? target.getBirthDate().toString() : null)
                            .gender(target.getGender() != null ? target.getGender().name() : null)
                            .sourceIndividualId(match.getSourceIndividualId())
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<MergePreviewResponse.RelationshipInfo> findNewRelationships(FamilyTree sourceTree,
            FamilyTree targetTree, List<IndividualMatch> matches) {
        // Simplified - would need more complex logic for full implementation
        return new ArrayList<>();
    }

    private List<MergePreviewResponse.ValidationError> validateDataIntegrity(
            List<MergePreviewResponse.IndividualInfo> newIndividuals,
            List<MergePreviewResponse.RelationshipInfo> newRelationships) {
        List<MergePreviewResponse.ValidationError> errors = new ArrayList<>();
        
        // Validate birth/death dates
        for (MergePreviewResponse.IndividualInfo ind : newIndividuals) {
            if (ind.getBirthDate() != null && ind.getDeathDate() != null) {
                LocalDate birth = LocalDate.parse(ind.getBirthDate());
                LocalDate death = LocalDate.parse(ind.getDeathDate());
                if (death.isBefore(birth)) {
                    errors.add(MergePreviewResponse.ValidationError.builder()
                            .code("INVALID_DATES")
                            .message("Death date cannot be before birth date: " + ind.getName())
                            .entityId(ind.getId())
                            .blocking(true)
                            .build());
                }
            }
        }
        
        return errors;
    }

    private MergePreviewResponse buildPreviewResponse(UUID targetTreeId, TreeMergeRequest request,
            List<IndividualMatch> matches, List<MergePreviewResponse.IndividualInfo> newIndividuals,
            List<MergePreviewResponse.IndividualInfo> updatedIndividuals,
            List<MergePreviewResponse.RelationshipInfo> newRelationships,
            List<MergeConflict> conflicts, List<MergePreviewResponse.ValidationError> errors,
            List<String> warnings, List<MergePreviewResponse.IndividualPreview> individualPreviews) {
        
        boolean canMerge = errors.stream().noneMatch(MergePreviewResponse.ValidationError::isBlocking);
        
        return MergePreviewResponse.builder()
                .targetTreeId(targetTreeId)
                .sourceTreeId(request.getSourceTreeId())
                .strategy(request.getStrategy())
                .isPreview(true)
                .summary(MergePreviewResponse.MergeSummary.builder()
                        .totalMatchedIndividuals(matches.size())
                        .totalNewIndividuals(newIndividuals.size())
                        .totalUpdatedIndividuals(updatedIndividuals.size())
                        .totalNewRelationships(newRelationships.size())
                        .totalConflicts(conflicts.size())
                        .totalErrors((int) errors.stream().filter(MergePreviewResponse.ValidationError::isBlocking).count())
                        .totalWarnings(warnings.size())
                        .build())
                .matchedIndividuals(matches)
                .newIndividuals(newIndividuals)
                .updatedIndividuals(updatedIndividuals)
                .newRelationships(newRelationships)
                .conflicts(conflicts)
                .individualPreviews(individualPreviews)
                .validationErrors(errors)
                .warnings(warnings)
                .canMerge(canMerge)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Build detailed individual previews for interactive selection UI
     */
    private List<MergePreviewResponse.IndividualPreview> buildIndividualPreviews(
            FamilyTree sourceTree, FamilyTree targetTree,
            List<IndividualMatch> matches, List<MergeConflict> allConflicts) {
        
        List<MergePreviewResponse.IndividualPreview> previews = new ArrayList<>();
        
        // Map for quick lookup of matches by source ID
        Map<UUID, IndividualMatch> matchesBySourceId = matches.stream()
                .collect(Collectors.toMap(IndividualMatch::getSourceIndividualId, m -> m));
        
        // Map conflicts by source individual ID
        Map<UUID, List<MergeConflict>> conflictsBySourceId = allConflicts.stream()
                .collect(Collectors.groupingBy(MergeConflict::getSourceIndividualId));
        
        // Get all source individuals
        List<Individual> sourceIndividuals = individualRepository.findByTreeId(sourceTree.getId());
        
        // Get all source relationships for grouping
        List<Relationship> sourceRelationships = relationshipRepository.findByTreeId(sourceTree.getId());
        
        // Get all target relationships for checking existence
        List<Relationship> targetRelationships = relationshipRepository.findByTreeId(targetTree.getId());
        
        for (Individual source : sourceIndividuals) {
            IndividualMatch match = matchesBySourceId.get(source.getId());
            Individual target = null;
            
            if (match != null) {
                target = individualRepository.findById(match.getTargetIndividualId()).orElse(null);
            }
            
            // Build conflicts for this individual
            List<MergeConflict> individualConflicts = conflictsBySourceId.getOrDefault(source.getId(), new ArrayList<>());
            List<MergePreviewResponse.FieldConflict> fieldConflicts = individualConflicts.stream()
                    .map(c -> MergePreviewResponse.FieldConflict.builder()
                            .field(c.getField())
                            .sourceValue(c.getSourceValue())
                            .targetValue(c.getTargetValue())
                            .build())
                    .collect(Collectors.toList());
            
            // Build relationships for this individual (where they are individual1 or individual2)
            List<MergePreviewResponse.RelationshipPreview> relPreviews = new ArrayList<>();
            for (Relationship rel : sourceRelationships) {
                if (rel.getIndividual1().getId().equals(source.getId())) {
                    // This individual is the parent/first party
                    Individual relatedPerson = rel.getIndividual2();
                    boolean existsInTarget = checkRelationshipExistsInTarget(
                            match, matchesBySourceId.get(relatedPerson.getId()), 
                            rel.getType(), targetRelationships);
                    
                    relPreviews.add(MergePreviewResponse.RelationshipPreview.builder()
                            .sourceRelationshipId(rel.getId())
                            .type(rel.getType().name())
                            .relatedPersonName(buildFullName(relatedPerson))
                            .relatedPersonSourceId(relatedPerson.getId())
                            .isParentRelation(true)
                            .existsInTarget(existsInTarget)
                            .build());
                } else if (rel.getIndividual2().getId().equals(source.getId())) {
                    // This individual is the child/second party
                    Individual relatedPerson = rel.getIndividual1();
                    boolean existsInTarget = checkRelationshipExistsInTarget(
                            matchesBySourceId.get(relatedPerson.getId()), match,
                            rel.getType(), targetRelationships);
                    
                    relPreviews.add(MergePreviewResponse.RelationshipPreview.builder()
                            .sourceRelationshipId(rel.getId())
                            .type(rel.getType().name())
                            .relatedPersonName(buildFullName(relatedPerson))
                            .relatedPersonSourceId(relatedPerson.getId())
                            .isParentRelation(false)
                            .existsInTarget(existsInTarget)
                            .build());
                }
            }
            
            // Determine match type string
            String matchType = "NEW";
            int matchScore = 0;
            if (match != null) {
                matchType = match.getMatchType().name();
                matchScore = match.getMatchScore();
            }
            
            MergePreviewResponse.IndividualPreview preview = MergePreviewResponse.IndividualPreview.builder()
                    .sourceIndividualId(source.getId())
                    .sourceName(buildFullName(source))
                    .sourceBirthDate(source.getBirthDate() != null ? source.getBirthDate().toString() : null)
                    .sourceDeathDate(source.getDeathDate() != null ? source.getDeathDate().toString() : null)
                    .sourceGender(source.getGender() != null ? source.getGender().name() : null)
                    .targetIndividualId(target != null ? target.getId() : null)
                    .targetName(target != null ? buildFullName(target) : null)
                    .targetBirthDate(target != null && target.getBirthDate() != null ? target.getBirthDate().toString() : null)
                    .targetDeathDate(target != null && target.getDeathDate() != null ? target.getDeathDate().toString() : null)
                    .matchType(matchType)
                    .matchScore(matchScore)
                    .isNew(match == null)
                    .hasConflicts(!fieldConflicts.isEmpty())
                    .conflicts(fieldConflicts)
                    .relationships(relPreviews)
                    .build();
            
            previews.add(preview);
        }
        
        return previews;
    }

    /**
     * Check if a relationship already exists in target tree
     */
    private boolean checkRelationshipExistsInTarget(IndividualMatch match1, IndividualMatch match2,
            RelationshipType type, List<Relationship> targetRelationships) {
        if (match1 == null || match2 == null) return false;
        
        UUID targetId1 = match1.getTargetIndividualId();
        UUID targetId2 = match2.getTargetIndividualId();
        
        return targetRelationships.stream().anyMatch(rel ->
                rel.getType() == type &&
                rel.getIndividual1().getId().equals(targetId1) &&
                rel.getIndividual2().getId().equals(targetId2));
    }

    private MergePreviewResponse buildErrorResponse(UUID targetTreeId, TreeMergeRequest request,
            ValidationResult validation) {
        return MergePreviewResponse.builder()
                .targetTreeId(targetTreeId)
                .sourceTreeId(request.getSourceTreeId())
                .strategy(request.getStrategy())
                .isPreview(true)
                .validationErrors(validation.getErrors())
                .warnings(validation.getWarnings())
                .canMerge(false)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    private Individual cloneIndividual(Individual source, FamilyTree targetTree) {
        return Individual.builder()
                .tree(targetTree)
                .givenName(source.getGivenName())
                .middleName(source.getMiddleName())
                .surname(source.getSurname())
                .suffix(source.getSuffix())
                .gender(source.getGender())
                .birthDate(source.getBirthDate())
                .birthPlace(source.getBirthPlace())
                .deathDate(source.getDeathDate())
                .deathPlace(source.getDeathPlace())
                .biography(source.getBiography())
                .notes(source.getNotes())
                .facebookLink(source.getFacebookLink())
                .phoneNumber(source.getPhoneNumber())
                .build();
    }

    private boolean updateFromSource(IndividualMatch match, ConflictResolution resolution) {
        Individual source = individualRepository.findById(match.getSourceIndividualId()).orElse(null);
        Individual target = individualRepository.findById(match.getTargetIndividualId()).orElse(null);
        
        if (source == null || target == null) return false;
        
        boolean updated = false;
        
        // Update empty fields from source
        if (target.getBiography() == null && source.getBiography() != null) {
            target.setBiography(source.getBiography());
            updated = true;
        }
        if (target.getNotes() == null && source.getNotes() != null) {
            target.setNotes(source.getNotes());
            updated = true;
        }
        
        // For THEIRS, overwrite all
        if (resolution == ConflictResolution.THEIRS) {
            if (source.getBirthPlace() != null) target.setBirthPlace(source.getBirthPlace());
            if (source.getDeathPlace() != null) target.setDeathPlace(source.getDeathPlace());
            if (source.getBiography() != null) target.setBiography(source.getBiography());
            updated = true;
        }
        
        if (updated) {
            individualRepository.save(target);
        }
        
        return updated;
    }

    private boolean hasEditPermission(FamilyTree tree, User user) {
        if (user.isAdmin()) return true;
        if (tree.getOwner().getId().equals(user.getId())) return true;
        if (tree.getAdmins().stream().anyMatch(a -> a.getId().equals(user.getId()))) return true;
        return permissionRepository.existsByTreeIdAndUserIdAndPermission(
                tree.getId(), user.getId(), PermissionRole.EDITOR);
    }

    private boolean hasViewPermission(FamilyTree tree, User user) {
        if (user.isAdmin()) return true;
        if (tree.getOwner().getId().equals(user.getId())) return true;
        if (tree.getAdmins().stream().anyMatch(a -> a.getId().equals(user.getId()))) return true;
        return permissionRepository.existsByTreeIdAndUserId(tree.getId(), user.getId());
    }

    private boolean areTreesCloneRelated(FamilyTree tree1, FamilyTree tree2) {
        // Check if one was cloned from the other
        if (tree1.getSourceTreeId() != null && tree1.getSourceTreeId().equals(tree2.getId())) return true;
        if (tree2.getSourceTreeId() != null && tree2.getSourceTreeId().equals(tree1.getId())) return true;
        // Check if they share a common source
        if (tree1.getSourceTreeId() != null && tree1.getSourceTreeId().equals(tree2.getSourceTreeId())) return true;
        return false;
    }

    private boolean wouldCreateCircularRelationship(FamilyTree target, FamilyTree source) {
        // Prevent merging if it would create a circular clone relationship
        // For now, simple check - in real implementation would be more sophisticated
        return false;
    }

    private String buildFullName(Individual individual) {
        StringBuilder name = new StringBuilder();
        if (individual.getSurname() != null) name.append(individual.getSurname());
        if (individual.getGivenName() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(individual.getGivenName());
        }
        return name.toString();
    }

    private IndividualMatch.MatchType getMatchType(int score) {
        if (score >= 180) return IndividualMatch.MatchType.HIGH_CONFIDENCE;
        if (score >= 130) return IndividualMatch.MatchType.MEDIUM_CONFIDENCE;
        return IndividualMatch.MatchType.LOW_CONFIDENCE;
    }

    private String getMatchReason(Individual source, Individual target) {
        StringBuilder reason = new StringBuilder();
        if (buildFullName(source).equalsIgnoreCase(buildFullName(target))) {
            reason.append("Names match");
        }
        if (source.getBirthDate() != null && source.getBirthDate().equals(target.getBirthDate())) {
            if (reason.length() > 0) reason.append(", ");
            reason.append("Birth dates match");
        }
        return reason.toString();
    }

    private MergePreviewResponse.IndividualInfo toIndividualInfo(Individual ind) {
        return MergePreviewResponse.IndividualInfo.builder()
                .id(ind.getId())
                .name(buildFullName(ind))
                .birthDate(ind.getBirthDate() != null ? ind.getBirthDate().toString() : null)
                .deathDate(ind.getDeathDate() != null ? ind.getDeathDate().toString() : null)
                .gender(ind.getGender() != null ? ind.getGender().name() : null)
                .sourceIndividualId(ind.getId())
                .build();
    }

    // Inner class for validation result
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class ValidationResult {
        private boolean valid;
        private List<MergePreviewResponse.ValidationError> errors;
        private List<String> warnings;
        private FamilyTree targetTree;
        private FamilyTree sourceTree;
        private User user;
    }
}
