package com.familytree.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.UUID;

/**
 * Request DTO for linking a user to an individual in a tree
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTreeProfileRequest {

    @NotNull(message = "Individual ID is required")
    private UUID individualId;
}
