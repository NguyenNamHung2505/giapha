package com.familytree.controller;

import com.familytree.dto.collaboration.InvitationResponse;
import com.familytree.dto.collaboration.InviteCollaboratorRequest;
import com.familytree.dto.collaboration.PermissionResponse;
import com.familytree.model.PermissionRole;
import com.familytree.security.UserPrincipal;
import com.familytree.service.InvitationService;
import com.familytree.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for collaboration and permission management
 */
@RestController
@RequestMapping("/api/trees/{treeId}/collaboration")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
@RequiredArgsConstructor
public class CollaborationController {

    private final InvitationService invitationService;
    private final PermissionService permissionService;

    /**
     * Invite a collaborator to the tree
     * POST /api/trees/{treeId}/collaboration/invite
     */
    @PostMapping("/invite")
    public ResponseEntity<InvitationResponse> inviteCollaborator(
            @PathVariable String treeId,
            @Valid @RequestBody InviteCollaboratorRequest request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            UUID treeUuid = UUID.fromString(treeId);
            UUID userId = userPrincipal.getId();

            InvitationResponse invitation = invitationService.inviteCollaborator(treeUuid, userId, request);

            return ResponseEntity.status(HttpStatus.CREATED).body(invitation);

        } catch (RuntimeException e) {
            log.error("Error inviting collaborator: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get all collaborators for a tree
     * GET /api/trees/{treeId}/collaboration/collaborators
     */
    @GetMapping("/collaborators")
    public ResponseEntity<List<PermissionResponse>> getCollaborators(
            @PathVariable String treeId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            UUID treeUuid = UUID.fromString(treeId);
            UUID userId = userPrincipal.getId();

            List<PermissionResponse> collaborators = permissionService.getTreeCollaborators(treeUuid, userId);

            return ResponseEntity.ok(collaborators);

        } catch (RuntimeException e) {
            log.error("Error getting collaborators: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Get all invitations for a tree
     * GET /api/trees/{treeId}/collaboration/invitations
     */
    @GetMapping("/invitations")
    public ResponseEntity<List<InvitationResponse>> getInvitations(
            @PathVariable String treeId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            UUID treeUuid = UUID.fromString(treeId);
            UUID userId = userPrincipal.getId();

            List<InvitationResponse> invitations = invitationService.getTreeInvitations(treeUuid, userId);

            return ResponseEntity.ok(invitations);

        } catch (RuntimeException e) {
            log.error("Error getting invitations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Update collaborator role
     * PUT /api/trees/{treeId}/collaboration/collaborators/{collaboratorId}/role
     */
    @PutMapping("/collaborators/{collaboratorId}/role")
    public ResponseEntity<Void> updateCollaboratorRole(
            @PathVariable String treeId,
            @PathVariable String collaboratorId,
            @RequestParam PermissionRole role,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            UUID treeUuid = UUID.fromString(treeId);
            UUID collaboratorUuid = UUID.fromString(collaboratorId);
            UUID userId = userPrincipal.getId();

            permissionService.updateCollaboratorRole(treeUuid, userId, collaboratorUuid, role);

            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            log.error("Error updating collaborator role: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Remove collaborator
     * DELETE /api/trees/{treeId}/collaboration/collaborators/{collaboratorId}
     */
    @DeleteMapping("/collaborators/{collaboratorId}")
    public ResponseEntity<Void> removeCollaborator(
            @PathVariable String treeId,
            @PathVariable String collaboratorId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            UUID treeUuid = UUID.fromString(treeId);
            UUID collaboratorUuid = UUID.fromString(collaboratorId);
            UUID userId = userPrincipal.getId();

            permissionService.removeCollaborator(treeUuid, userId, collaboratorUuid);

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            log.error("Error removing collaborator: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Leave a tree
     * POST /api/trees/{treeId}/collaboration/leave
     */
    @PostMapping("/leave")
    public ResponseEntity<Void> leaveTree(
            @PathVariable String treeId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            UUID treeUuid = UUID.fromString(treeId);
            UUID userId = userPrincipal.getId();

            permissionService.leaveTree(treeUuid, userId);

            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            log.error("Error leaving tree: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Cancel an invitation
     * DELETE /api/trees/{treeId}/collaboration/invitations/{invitationId}
     */
    @DeleteMapping("/invitations/{invitationId}")
    public ResponseEntity<Void> cancelInvitation(
            @PathVariable String treeId,
            @PathVariable String invitationId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            UUID invitationUuid = UUID.fromString(invitationId);
            UUID userId = userPrincipal.getId();

            invitationService.cancelInvitation(invitationUuid, userId);

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            log.error("Error cancelling invitation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }
}
