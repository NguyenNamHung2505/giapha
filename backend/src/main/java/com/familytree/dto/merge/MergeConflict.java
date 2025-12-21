package com.familytree.dto.merge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a conflict between source and target data for an individual
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeConflict {

    private UUID conflictId;
    
    /**
     * ID of the individual in target tree
     */
    private UUID targetIndividualId;
    
    /**
     * ID of the matching individual in source tree
     */
    private UUID sourceIndividualId;
    
    /**
     * Name of the individual for display
     */
    private String individualName;
    
    /**
     * Type of conflict
     */
    private ConflictType conflictType;
    
    /**
     * Field that has conflict
     */
    private String field;
    
    /**
     * Current value in target tree
     */
    private String targetValue;
    
    /**
     * Value in source tree
     */
    private String sourceValue;
    
    /**
     * Severity of the conflict
     */
    private ConflictSeverity severity;
    
    /**
     * Suggestion for resolution
     */
    private String suggestion;

    public enum ConflictType {
        /**
         * Same field has different values
         */
        DATA_MISMATCH,
        
        /**
         * Individual exists in target, deleted in source
         */
        DELETED_IN_SOURCE,
        
        /**
         * Individual deleted in target, modified in source
         */
        DELETED_IN_TARGET,
        
        /**
         * Relationship type differs
         */
        RELATIONSHIP_MISMATCH,
        
        /**
         * Duplicate individual detected
         */
        POSSIBLE_DUPLICATE
    }

    public enum ConflictSeverity {
        /**
         * Just informational, can be auto-resolved
         */
        INFO,
        
        /**
         * Should review but can proceed
         */
        WARNING,
        
        /**
         * Must resolve before merge
         */
        ERROR
    }
}
