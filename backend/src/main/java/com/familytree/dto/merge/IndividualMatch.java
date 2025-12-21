package com.familytree.dto.merge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Represents a matched individual between source and target trees
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndividualMatch {

    /**
     * ID of the individual in target tree
     */
    private UUID targetIndividualId;
    
    /**
     * ID of the matching individual in source tree
     */
    private UUID sourceIndividualId;
    
    /**
     * Full name of person in target tree
     */
    private String targetName;
    
    /**
     * Full name of person in source tree
     */
    private String sourceName;
    
    /**
     * Match confidence score (0-100)
     */
    private int matchScore;
    
    /**
     * Type of match
     */
    private MatchType matchType;
    
    /**
     * Reason for the match
     */
    private String matchReason;
    
    /**
     * Whether this is from a clone mapping (exact match)
     */
    private boolean clonedMatch;

    public enum MatchType {
        /**
         * Exact clone mapping exists
         */
        CLONE_MAPPING,
        
        /**
         * High confidence match (name + birth date)
         */
        HIGH_CONFIDENCE,
        
        /**
         * Medium confidence (name similar, partial date)
         */
        MEDIUM_CONFIDENCE,
        
        /**
         * Low confidence (fuzzy name match only)
         */
        LOW_CONFIDENCE,
        
        /**
         * Manual match by user
         */
        MANUAL
    }
}
