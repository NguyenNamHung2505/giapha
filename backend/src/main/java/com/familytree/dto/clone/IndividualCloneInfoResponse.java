package com.familytree.dto.clone;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO containing clone information for an individual
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IndividualCloneInfoResponse {

    /**
     * The individual ID being queried
     */
    private UUID individualId;

    /**
     * The individual's full name
     */
    private String individualName;

    /**
     * Whether this individual has been cloned to other trees
     */
    private boolean hasClones;

    /**
     * Whether this individual is a clone from another tree
     */
    private boolean isClone;

    /**
     * Whether this individual is the ROOT of a clone relationship
     * True if: this person was selected as root when creating a cloned tree (source side)
     * OR this person is the root individual in a cloned tree (clone side)
     * This is used to only show "View in other trees" for the main cloned person,
     * not for all family members who were copied along.
     */
    private boolean isRootClonedPerson;

    /**
     * Information about trees this individual has been cloned to
     */
    private List<ClonedTreeInfo> clonedToTrees;

    /**
     * Information about the source if this individual is a clone
     */
    private SourceInfo sourceInfo;

    /**
     * All trees where this person (or their clone) exists
     * Used for "View in Tree" dropdown - allows switching between trees
     */
    private List<TreeLocation> allTreeLocations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TreeLocation {
        private UUID treeId;
        private String treeName;
        private UUID individualId;
        private boolean isCurrentTree;
        private boolean isSourceTree;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClonedTreeInfo {
        private UUID clonedTreeId;
        private String clonedTreeName;
        private UUID clonedIndividualId;
        private String clonedIndividualName;
        private boolean isRootOfClone;
        private LocalDateTime clonedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceInfo {
        private UUID sourceTreeId;
        private String sourceTreeName;
        private UUID sourceIndividualId;
        private String sourceIndividualName;
        private LocalDateTime clonedAt;
    }
}
