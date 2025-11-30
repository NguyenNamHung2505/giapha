package com.familytree.dto.clone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO containing clone information for a tree
 * Shows if this tree is a clone and lists all related trees
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeCloneInfoResponse {

    /**
     * The tree ID being queried
     */
    private UUID treeId;

    /**
     * The tree name
     */
    private String treeName;

    /**
     * Whether this tree is a clone of another tree
     */
    private boolean isClone;

    /**
     * Whether this tree has been cloned to create other trees
     */
    private boolean hasClones;

    /**
     * Info about the source tree if this is a clone
     */
    private SourceTreeInfo sourceTreeInfo;

    /**
     * List of trees that were cloned from this tree
     */
    private List<ClonedTreeInfo> clonedTrees;

    /**
     * All related trees (for navigation dropdown)
     */
    private List<RelatedTreeInfo> allRelatedTrees;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceTreeInfo {
        private UUID sourceTreeId;
        private String sourceTreeName;
        private UUID sourceIndividualId;
        private String sourceIndividualName;
        private LocalDateTime clonedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClonedTreeInfo {
        private UUID clonedTreeId;
        private String clonedTreeName;
        private UUID rootIndividualId;
        private String rootIndividualName;
        private LocalDateTime clonedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RelatedTreeInfo {
        private UUID treeId;
        private String treeName;
        private boolean isCurrentTree;
        private boolean isSourceTree;
        private LocalDateTime clonedAt;
    }
}
