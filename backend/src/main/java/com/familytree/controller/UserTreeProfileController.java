package com.familytree.controller;

import com.familytree.dto.user.UserTreeProfileRequest;
import com.familytree.dto.user.UserTreeProfileResponse;
import com.familytree.service.UserTreeProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

/**
 * REST controller for managing user-to-individual profile mappings
 */
@RestController
@RequestMapping("/api/trees/{treeId}/my-profile")
@RequiredArgsConstructor
@Slf4j
public class UserTreeProfileController {

    private final UserTreeProfileService userTreeProfileService;

    /**
     * Link current user to an individual in the tree
     * POST /api/trees/{treeId}/my-profile
     */
    @PostMapping
    public ResponseEntity<UserTreeProfileResponse> linkToIndividual(
            @PathVariable UUID treeId,
            @Valid @RequestBody UserTreeProfileRequest request,
            Authentication authentication) {

        log.info("Linking user {} to individual {} in tree {}",
                authentication.getName(), request.getIndividualId(), treeId);

        UserTreeProfileResponse response = userTreeProfileService.linkUserToIndividual(
                treeId, request.getIndividualId(), authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get current user's profile mapping for the tree
     * GET /api/trees/{treeId}/my-profile
     */
    @GetMapping
    public ResponseEntity<UserTreeProfileResponse> getMyProfile(
            @PathVariable UUID treeId,
            Authentication authentication) {

        log.info("Getting profile for user {} in tree {}", authentication.getName(), treeId);

        UserTreeProfileResponse response = userTreeProfileService.getUserProfileOrNull(
                treeId, authentication.getName());

        if (response == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Remove current user's profile mapping
     * DELETE /api/trees/{treeId}/my-profile
     */
    @DeleteMapping
    public ResponseEntity<Void> unlinkFromIndividual(
            @PathVariable UUID treeId,
            Authentication authentication) {

        log.info("Unlinking user {} from tree {}", authentication.getName(), treeId);

        userTreeProfileService.unlinkUserFromIndividual(treeId, authentication.getName());

        return ResponseEntity.noContent().build();
    }
}
