package com.familytree.dto.merge;

import com.familytree.dto.individual.IndividualResponse;
import com.familytree.dto.relationship.RelationshipResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for merge preview and results
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergePreviewResponse {

    /**
     * Target tree ID (merging INTO)
     */
    private UUID targetTreeId;
    
    /**
     * Source tree ID (merging FROM)
     */
    private UUID sourceTreeId;
    
    /**
     * Merge strategy used
     */
    private MergeStrategy strategy;
    
    /**
     * Whether this is just a preview
     */
    private boolean isPreview;
    
    /**
     * Summary statistics
     */
    private MergeSummary summary;
    
    /**
     * Individuals matched between source and target
     */
    private List<IndividualMatch> matchedIndividuals;
    
    /**
     * New individuals to be added to target
     */
    private List<IndividualInfo> newIndividuals;
    
    /**
     * Individuals to be updated in target
     */
    private List<IndividualInfo> updatedIndividuals;
    
    /**
     * New relationships to be added
     */
    private List<RelationshipInfo> newRelationships;
    
    /**
     * Conflicts that need resolution
     */
    private List<MergeConflict> conflicts;
    
    /**
     * Detailed preview of each source individual for interactive selection
     * Includes mapping to target, conflicts, and relationships
     */
    private List<IndividualPreview> individualPreviews;
    
    /**
     * Validation errors (must be resolved before merge)
     */
    private List<ValidationError> validationErrors;
    
    /**
     * Warnings (can proceed but should review)
     */
    private List<String> warnings;
    
    /**
     * Whether merge can proceed (no blocking errors)
     */
    private boolean canMerge;
    
    /**
     * Timestamp when preview was generated
     */
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MergeSummary {
        private int totalMatchedIndividuals;
        private int totalNewIndividuals;
        private int totalUpdatedIndividuals;
        private int totalNewRelationships;
        private int totalConflicts;
        private int totalErrors;
        private int totalWarnings;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndividualInfo {
        private UUID id;
        private String name;
        private String birthDate;
        private String deathDate;
        private String gender;
        private UUID sourceIndividualId; // For tracking origin
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationshipInfo {
        private UUID id;
        private String individual1Name;
        private String individual2Name;
        private String type;
        private UUID sourceRelationshipId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String code;
        private String message;
        private String field;
        private UUID entityId;
        private boolean blocking; // If true, merge cannot proceed
    }

    /**
     * Detailed preview of each individual for interactive selection
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndividualPreview {
        // Source individual info
        private UUID sourceIndividualId;
        private String sourceName;
        private String sourceBirthDate;
        private String sourceDeathDate;
        private String sourceGender;
        
        // Target individual info (null if new)
        private UUID targetIndividualId;
        private String targetName;
        private String targetBirthDate;
        private String targetDeathDate;
        
        // Match info
        private String matchType; // CLONE_MAPPING, EXACT, HIGH, MEDIUM, LOW, NEW
        private int matchScore;
        private boolean isNew; // true if no match found
        private boolean hasConflicts;
        
        // Conflicts for this individual
        private List<FieldConflict> conflicts;
        
        // Relationships from source (grouped under this individual)
        private List<RelationshipPreview> relationships;
    }

    /**
     * Field-level conflict for an individual
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldConflict {
        private String field;
        private String sourceValue;
        private String targetValue;
        private String resolvedValue; // null until resolved
    }

    /**
     * Relationship preview grouped under individual
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelationshipPreview {
        private UUID sourceRelationshipId;
        private String type;
        private String relatedPersonName;
        private UUID relatedPersonSourceId;
        private boolean isParentRelation; // true if this individual is the parent
        private boolean existsInTarget; // true if relationship already exists in target
    }
}

