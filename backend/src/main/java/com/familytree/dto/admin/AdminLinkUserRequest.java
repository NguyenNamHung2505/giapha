package com.familytree.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for admin to link a user to an individual in a tree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLinkUserRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotNull(message = "Individual ID is required")
    private UUID individualId;
}
