package com.familytree.dto.tree;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for ancestor tree data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AncestorTreeResponse {

    private AncestorNode root;
    private int totalAncestors;
    private int maxGeneration;

    /**
     * Represents a node in the ancestor tree
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AncestorNode {
        private UUID id;
        private String givenName;
        private String surname;
        private String suffix;
        private String fullName;
        private String gender;
        private LocalDate birthDate;
        private String birthPlace;
        private LocalDate deathDate;
        private String deathPlace;
        private String profilePictureUrl;
        private int generation; // 0 = self, 1 = parents, 2 = grandparents, etc.

        @Builder.Default
        private List<AncestorNode> parents = new ArrayList<>(); // Father and Mother
    }
}
