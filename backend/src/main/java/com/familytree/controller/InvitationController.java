package com.familytree.controller;

import com.familytree.dto.collaboration.InvitationResponse;
import com.familytree.security.UserPrincipal;
import com.familytree.service.InvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for invitation management
 */
@RestController
@RequestMapping("/api/invitations")
@Slf4j
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    /**
     * Get invitation details by token (public endpoint, before acceptance)
     * GET /api/invitations/{token}
     */
    @GetMapping("/{token}")
    public ResponseEntity<InvitationResponse> getInvitation(@PathVariable String token) {
        try {
            InvitationResponse invitation = invitationService.getInvitationByToken(token);
            return ResponseEntity.ok(invitation);
        } catch (RuntimeException e) {
            log.error("Error getting invitation: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Accept an invitation
     * POST /api/invitations/{token}/accept
     */
    @PostMapping("/{token}/accept")
    public ResponseEntity<Void> acceptInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            invitationService.acceptInvitation(token, userPrincipal.getId());
            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            log.error("Error accepting invitation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Decline an invitation
     * POST /api/invitations/{token}/decline
     */
    @PostMapping("/{token}/decline")
    public ResponseEntity<Void> declineInvitation(
            @PathVariable String token,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            invitationService.declineInvitation(token, userPrincipal.getId());
            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            log.error("Error declining invitation: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get current user's pending invitations
     * GET /api/invitations/my-invitations
     */
    @GetMapping("/my-invitations")
    public ResponseEntity<List<InvitationResponse>> getMyInvitations(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            List<InvitationResponse> invitations = invitationService
                    .getUserPendingInvitations(userPrincipal.getEmail());
            return ResponseEntity.ok(invitations);

        } catch (Exception e) {
            log.error("Error getting user invitations: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
