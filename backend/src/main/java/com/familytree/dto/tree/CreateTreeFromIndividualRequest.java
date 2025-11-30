package com.familytree.dto.tree;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.UUID;

/**
 * Request DTO for creating a new family tree from an individual
 * This clones the individual's ancestors and descendants into a new tree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTreeFromIndividualRequest {

    @NotNull(message = "Source tree ID is required")
    private UUID sourceTreeId;

    @NotNull(message = "Root individual ID is required")
    private UUID rootIndividualId;

    @NotBlank(message = "New tree name is required")
    @Size(max = 255, message = "Tree name must not exceed 255 characters")
    private String newTreeName;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String newTreeDescription;

    @Builder.Default
    private boolean includeMedia = true;
}
