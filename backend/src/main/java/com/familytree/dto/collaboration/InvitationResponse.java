package com.familytree.dto.collaboration;

import com.familytree.model.PermissionRole;
import com.familytree.model.TreeInvitation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response containing invitation details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationResponse {

    private String id;
    private String treeId;
    private String treeName;
    private String inviterName;
    private String inviterEmail;
    private String inviteeEmail;
    private PermissionRole role;
    private String token;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private String status;
    private boolean expired;

    /**
     * Create from entity
     */
    public static InvitationResponse fromEntity(TreeInvitation invitation) {
        return InvitationResponse.builder()
                .id(invitation.getId().toString())
                .treeId(invitation.getTree().getId().toString())
                .treeName(invitation.getTree().getName())
                .inviterName(invitation.getInviter().getName())
                .inviterEmail(invitation.getInviter().getEmail())
                .inviteeEmail(invitation.getInviteeEmail())
                .role(invitation.getRole())
                .token(invitation.getToken())
                .expiresAt(invitation.getExpiresAt())
                .createdAt(invitation.getCreatedAt())
                .status(invitation.getStatus().name())
                .expired(invitation.isExpired())
                .build();
    }
}
