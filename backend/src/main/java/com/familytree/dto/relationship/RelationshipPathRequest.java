package com.familytree.dto.relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request to find relationship path between two individuals
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelationshipPathRequest {

    @NotNull(message = "First individual ID is required")
    private UUID person1Id;

    @NotNull(message = "Second individual ID is required")
    private UUID person2Id;
}
