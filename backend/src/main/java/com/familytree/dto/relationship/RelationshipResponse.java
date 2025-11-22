package com.familytree.dto.relationship;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.familytree.model.RelationshipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for relationship information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelationshipResponse {

    private UUID id;

    private UUID treeId;

    private IndividualSummary individual1;

    private IndividualSummary individual2;

    private RelationshipType type;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * Nested DTO for individual summary in relationship response
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class IndividualSummary {
        private UUID id;
        private String givenName;
        private String surname;
        private String fullName;
        private LocalDate birthDate;
        private LocalDate deathDate;
    }
}
