package com.familytree.service;

import com.familytree.dto.collaboration.InvitationResponse;
import com.familytree.dto.collaboration.InviteCollaboratorRequest;
import com.familytree.model.*;
import com.familytree.repository.FamilyTreeRepository;
import com.familytree.repository.TreeInvitationRepository;
import com.familytree.repository.TreePermissionRepository;
import com.familytree.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing tree invitations and collaboration
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class InvitationService {

    private final TreeInvitationRepository invitationRepository;
    private final FamilyTreeRepository treeRepository;
    private final UserRepository userRepository;
    private final TreePermissionRepository permissionRepository;
    private final PermissionService permissionService;

    private static final int TOKEN_LENGTH = 32;
    private static final int INVITATION_VALIDITY_DAYS = 7;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Create an invitation to collaborate on a tree
     */
    @Transactional
    public InvitationResponse inviteCollaborator(UUID treeId, UUID inviterId, InviteCollaboratorRequest request) {
        // Verify tree exists and inviter has permission
        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Tree not found"));

        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new RuntimeException("Inviter not found"));

        // Only owner can invite collaborators
        if (!tree.getOwner().getId().equals(inviterId)) {
            throw new RuntimeException("Only the tree owner can invite collaborators");
        }

        // Validate role
        if (request.getRole() == PermissionRole.OWNER) {
            throw new RuntimeException("Cannot invite someone as owner");
        }

        // Check if user is inviting themselves
        if (inviter.getEmail().equalsIgnoreCase(request.getEmail())) {
            throw new RuntimeException("Cannot invite yourself");
        }

        // Check for existing pending invitation
        Optional<TreeInvitation> existingInvitation = invitationRepository
                .findPendingByTreeAndEmail(treeId, request.getEmail(), LocalDateTime.now());

        if (existingInvitation.isPresent()) {
            throw new RuntimeException("An invitation for this email already exists");
        }

        // Check if user already has access
        User existingUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (existingUser != null && permissionService.canViewTree(existingUser.getId(), treeId)) {
            throw new RuntimeException("User already has access to this tree");
        }

        // Generate secure token
        String token = generateSecureToken();

        // Create invitation
        TreeInvitation invitation = TreeInvitation.builder()
                .tree(tree)
                .inviter(inviter)
                .inviteeEmail(request.getEmail())
                .token(token)
                .role(request.getRole())
                .expiresAt(LocalDateTime.now().plusDays(INVITATION_VALIDITY_DAYS))
                .status(TreeInvitation.InvitationStatus.PENDING)
                .build();

        TreeInvitation saved = invitationRepository.save(invitation);

        log.info("Invitation created: {} invited {} to tree {} with role {}",
                inviter.getEmail(), request.getEmail(), tree.getName(), request.getRole());

        return InvitationResponse.fromEntity(saved);
    }

    /**
     * Accept an invitation
     */
    @Transactional
    public void acceptInvitation(String token, UUID userId) {
        TreeInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Validate invitation
        if (!invitation.isValid()) {
            if (invitation.isExpired()) {
                invitation.setStatus(TreeInvitation.InvitationStatus.EXPIRED);
                invitationRepository.save(invitation);
                throw new RuntimeException("Invitation has expired");
            }
            throw new RuntimeException("Invitation is no longer valid");
        }

        // Verify email matches
        if (!user.getEmail().equalsIgnoreCase(invitation.getInviteeEmail())) {
            throw new RuntimeException("This invitation is for a different email address");
        }

        // Check if user already has permission
        if (permissionService.canViewTree(userId, invitation.getTree().getId())) {
            throw new RuntimeException("You already have access to this tree");
        }

        // Create permission
        TreePermission permission = TreePermission.builder()
                .tree(invitation.getTree())
                .user(user)
                .role(invitation.getRole())
                .build();

        permissionRepository.save(permission);

        // Update invitation status
        invitation.setStatus(TreeInvitation.InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitation.setAcceptedByUser(user);
        invitationRepository.save(invitation);

        log.info("Invitation accepted: {} accepted invitation to tree {}",
                user.getEmail(), invitation.getTree().getName());
    }

    /**
     * Decline an invitation
     */
    @Transactional
    public void declineInvitation(String token, UUID userId) {
        TreeInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify email matches
        if (!user.getEmail().equalsIgnoreCase(invitation.getInviteeEmail())) {
            throw new RuntimeException("This invitation is for a different email address");
        }

        invitation.setStatus(TreeInvitation.InvitationStatus.DECLINED);
        invitationRepository.save(invitation);

        log.info("Invitation declined: {} declined invitation to tree {}",
                user.getEmail(), invitation.getTree().getName());
    }

    /**
     * Cancel an invitation (by inviter)
     */
    @Transactional
    public void cancelInvitation(UUID invitationId, UUID userId) {
        TreeInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        // Verify user is the inviter or tree owner
        if (!invitation.getInviter().getId().equals(userId) &&
            !invitation.getTree().getOwner().getId().equals(userId)) {
            throw new RuntimeException("Only the inviter or tree owner can cancel invitations");
        }

        invitation.setStatus(TreeInvitation.InvitationStatus.CANCELLED);
        invitationRepository.save(invitation);

        log.info("Invitation cancelled: invitation to {} for tree {}",
                invitation.getInviteeEmail(), invitation.getTree().getName());
    }

    /**
     * Get invitation by token
     */
    @Transactional(readOnly = true)
    public InvitationResponse getInvitationByToken(String token) {
        TreeInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invitation not found"));

        return InvitationResponse.fromEntity(invitation);
    }

    /**
     * Get all invitations for a tree
     */
    @Transactional(readOnly = true)
    public List<InvitationResponse> getTreeInvitations(UUID treeId, UUID userId) {
        // Verify user has permission to view invitations (must be owner)
        FamilyTree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Tree not found"));

        if (!tree.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Only the tree owner can view invitations");
        }

        List<TreeInvitation> invitations = invitationRepository.findByTreeId(treeId);

        return invitations.stream()
                .map(InvitationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get pending invitations for current user
     */
    @Transactional(readOnly = true)
    public List<InvitationResponse> getUserPendingInvitations(String email) {
        List<TreeInvitation> invitations = invitationRepository
                .findPendingByEmail(email, LocalDateTime.now());

        return invitations.stream()
                .map(InvitationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Clean up expired invitations (scheduled task)
     */
    @Transactional
    public void cleanupExpiredInvitations() {
        invitationRepository.deleteExpired(LocalDateTime.now());
        log.info("Cleaned up expired invitations");
    }

    /**
     * Generate a secure random token
     */
    private String generateSecureToken() {
        byte[] randomBytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
