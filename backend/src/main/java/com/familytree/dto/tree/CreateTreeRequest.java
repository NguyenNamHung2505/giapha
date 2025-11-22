package com.familytree.dto.tree;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * DTO for creating a new family tree
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTreeRequest {

    @NotBlank(message = "Tree name is required")
    @Size(min = 1, max = 255, message = "Tree name must be between 1 and 255 characters")
    private String name;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;
}
