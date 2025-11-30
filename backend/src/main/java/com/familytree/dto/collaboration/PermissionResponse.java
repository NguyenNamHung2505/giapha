package com.familytree.dto.collaboration;

import com.familytree.model.PermissionRole;
import com.familytree.model.TreePermission;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response containing permission details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionResponse {

    private String id;
    private String userId;
    private String userName;
    private String userEmail;
    private PermissionRole role;
    private LocalDateTime grantedAt;
    private boolean isOwner;
    private boolean isTreeAdmin;

    /**
     * Create from entity
     */
    public static PermissionResponse fromEntity(TreePermission permission) {
        return PermissionResponse.builder()
                .id(permission.getId().toString())
                .userId(permission.getUser().getId().toString())
                .userName(permission.getUser().getName())
                .userEmail(permission.getUser().getEmail())
                .role(permission.getRole())
                .grantedAt(permission.getGrantedAt())
                .isOwner(false)
                .isTreeAdmin(false)
                .build();
    }
}
