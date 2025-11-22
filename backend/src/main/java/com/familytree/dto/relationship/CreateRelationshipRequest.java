package com.familytree.dto.relationship;

import com.familytree.model.RelationshipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new relationship
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRelationshipRequest {

    @NotNull(message = "Individual 1 ID is required")
    private UUID individual1Id;

    @NotNull(message = "Individual 2 ID is required")
    private UUID individual2Id;

    @NotNull(message = "Relationship type is required")
    private RelationshipType type;

    private LocalDate startDate;

    private LocalDate endDate;
}
