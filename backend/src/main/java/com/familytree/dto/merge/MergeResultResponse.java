package com.familytree.dto.merge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for merge execution result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeResultResponse {

    /**
     * Whether merge was successful
     */
    private boolean success;
    
    /**
     * Target tree ID
     */
    private UUID targetTreeId;
    
    /**
     * Source tree ID
     */
    private UUID sourceTreeId;
    
    /**
     * Merge ID for tracking/undo
     */
    private UUID mergeId;
    
    /**
     * Summary of changes made
     */
    private MergeSummary summary;
    
    /**
     * Human readable message
     */
    private String message;
    
    /**
     * When merge was executed
     */
    private LocalDateTime mergedAt;
    
    /**
     * Whether merge can be undone
     */
    private boolean canUndo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MergeSummary {
        private int individualsAdded;
        private int individualsUpdated;
        private int relationshipsAdded;
        private int mediaFilesAdded;
        private int conflictsResolved;
    }
}
