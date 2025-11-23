package com.familytree.dto.collaboration;

import com.familytree.model.PermissionRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Request to invite a collaborator to a tree
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteCollaboratorRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Role is required")
    private PermissionRole role;

    private String message; // Optional personal message
}
