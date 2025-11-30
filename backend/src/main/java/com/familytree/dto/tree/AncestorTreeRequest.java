package com.familytree.dto.tree;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for fetching ancestor tree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AncestorTreeRequest {

    @NotNull(message = "Individual ID is required")
    private UUID individualId;

    @Min(value = 1, message = "Generations must be at least 1")
    @Max(value = 10, message = "Generations cannot exceed 10")
    @Builder.Default
    private Integer generations = 3;
}
