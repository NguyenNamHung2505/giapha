package com.familytree.dto.tree;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for creating a new family tree from an individual
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTreeFromIndividualResponse {

    private UUID newTreeId;
    private String newTreeName;
    private UUID rootIndividualId;  // New ID of the root individual in the new tree

    private int totalIndividuals;
    private int totalRelationships;
    private int totalMediaFiles;

    private UUID sourceTreeId;
    private UUID sourceIndividualId;  // Original ID from source tree
    private LocalDateTime clonedAt;

    private String message;
}
