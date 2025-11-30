package com.familytree.dto.relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response containing the calculated relationship between two individuals
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelationshipPathResponse {

    /**
     * First person in the relationship query
     */
    private PersonSummary person1;

    /**
     * Second person in the relationship query
     */
    private PersonSummary person2;

    /**
     * The relationship description from person1's perspective to person2
     * e.g., "person1 is the grandfather of person2"
     */
    private String relationshipFromPerson1;

    /**
     * The relationship description from person2's perspective to person1
     * e.g., "person2 is the grandson of person1"
     */
    private String relationshipFromPerson2;

    /**
     * Vietnamese relationship term from person1's perspective
     */
    private String relationshipFromPerson1Vi;

    /**
     * Vietnamese relationship term from person2's perspective
     */
    private String relationshipFromPerson2Vi;

    /**
     * The relationship type classification
     */
    private RelationshipCategory category;

    /**
     * Number of generations between the two people (0 for same generation)
     */
    private int generationDifference;

    /**
     * The path of individuals connecting person1 to person2
     */
    private List<PathStep> path;

    /**
     * Whether a relationship was found
     */
    private boolean relationshipFound;

    /**
     * Common ancestor if the relationship is collateral (e.g., cousins)
     */
    private PersonSummary commonAncestor;

    /**
     * Summary information about a person
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PersonSummary {
        private UUID id;
        private String fullName;
        private String givenName;
        private String surname;
        private String gender;
        private LocalDate birthDate;
        private LocalDate deathDate;
    }

    /**
     * A step in the relationship path
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PathStep {
        private PersonSummary person;
        private String relationshipToNext;
        private String relationshipToNextVi;
    }

    /**
     * Category of relationship
     */
    public enum RelationshipCategory {
        SELF,                   // Same person
        DIRECT_ANCESTOR,        // Parent, grandparent, etc.
        DIRECT_DESCENDANT,      // Child, grandchild, etc.
        SIBLING,               // Brother/sister
        SPOUSE,                // Husband/wife
        UNCLE_AUNT,            // Parent's sibling
        NEPHEW_NIECE,          // Sibling's child
        COUSIN,                // Various cousin relationships
        GRANDUNCLE_GRANDAUNT,  // Grandparent's sibling
        GRANDNEPHEW_GRANDNIECE, // Sibling's grandchild
        IN_LAW,                // Related through marriage
        STEP_FAMILY,           // Step relationships
        NOT_RELATED            // No blood or marriage relation found
    }
}
