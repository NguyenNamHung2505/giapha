package com.familytree.dto.merge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for tree merge operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeMergeRequest {

    /**
     * ID of the source tree to merge FROM
     */
    @NotNull(message = "Source tree ID is required")
    private UUID sourceTreeId;

    /**
     * Merge strategy to use
     */
    @NotNull(message = "Merge strategy is required")
    @Builder.Default
    private MergeStrategy strategy = MergeStrategy.IMPORT;

    /**
     * How to resolve conflicts
     */
    @Builder.Default
    private ConflictResolution conflictResolution = ConflictResolution.OURS;

    /**
     * For IMPORT strategy: specific individuals to import
     * If null/empty, all individuals will be considered
     */
    private List<UUID> selectedIndividualIds;

    /**
     * Whether to include media files in the merge
     */
    @Builder.Default
    private boolean includeMedia = true;

    /**
     * Whether to do a dry-run (preview only)
     */
    @Builder.Default
    private boolean previewOnly = false;

    /**
     * Manual conflict resolutions (for MANUAL conflict resolution)
     * Map of conflictId -> resolution choice
     */
    private List<ManualConflictResolution> manualResolutions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ManualConflictResolution {
        private UUID conflictId;
        private String field;
        private String chosenValue;
        private boolean useSource; // true = use source value, false = keep target
    }
}
