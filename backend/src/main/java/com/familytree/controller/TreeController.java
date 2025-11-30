package com.familytree.controller;

import com.familytree.dto.clone.IndividualCloneInfoResponse;
import com.familytree.dto.clone.TreeCloneInfoResponse;
import com.familytree.dto.tree.CreateTreeFromIndividualRequest;
import com.familytree.dto.tree.CreateTreeFromIndividualResponse;
import com.familytree.dto.tree.CreateTreeRequest;
import com.familytree.dto.tree.TreeResponse;
import com.familytree.dto.tree.UpdateTreeRequest;
import com.familytree.service.TreeCloneService;
import com.familytree.service.TreeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

/**
 * REST controller for family tree management
 */
@RestController
@RequestMapping("/api/trees")
@RequiredArgsConstructor
@Slf4j
public class TreeController {

    private final TreeService treeService;
    private final TreeCloneService treeCloneService;

    /**
     * Create a new family tree
     * POST /api/trees
     */
    @PostMapping
    public ResponseEntity<TreeResponse> createTree(
            @Valid @RequestBody CreateTreeRequest request,
            Authentication authentication) {

        log.info("Creating tree for user: {}", authentication.getName());
        TreeResponse response = treeService.createTree(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all trees accessible by the current user
     * GET /api/trees
     */
    @GetMapping
    public ResponseEntity<Page<TreeResponse>> listTrees(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Listing trees for user: {}", authentication.getName());
        Page<TreeResponse> trees = treeService.listTrees(authentication.getName(), pageable);
        return ResponseEntity.ok(trees);
    }

    /**
     * Get a specific tree by ID
     * GET /api/trees/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TreeResponse> getTree(
            @PathVariable UUID id,
            Authentication authentication) {

        log.info("Fetching tree {} for user: {}", id, authentication.getName());
        TreeResponse response = treeService.getTree(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Update a tree
     * PUT /api/trees/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TreeResponse> updateTree(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTreeRequest request,
            Authentication authentication) {

        log.info("Updating tree {} for user: {}", id, authentication.getName());
        TreeResponse response = treeService.updateTree(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a tree
     * DELETE /api/trees/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTree(
            @PathVariable UUID id,
            Authentication authentication) {

        log.info("Deleting tree {} for user: {}", id, authentication.getName());
        treeService.deleteTree(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * Create a new tree from an individual (clone ancestors and descendants)
     * POST /api/trees/from-individual
     */
    @PostMapping("/from-individual")
    public ResponseEntity<CreateTreeFromIndividualResponse> createTreeFromIndividual(
            @Valid @RequestBody CreateTreeFromIndividualRequest request,
            Authentication authentication) {

        log.info("Creating tree from individual {} in tree {} for user: {}",
                request.getRootIndividualId(), request.getSourceTreeId(), authentication.getName());

        CreateTreeFromIndividualResponse response = treeCloneService.createTreeFromIndividual(
                request, authentication.getName());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Check if an individual has already been exported to a separate tree
     * GET /api/trees/{treeId}/individuals/{individualId}/is-exported
     */
    @GetMapping("/{treeId}/individuals/{individualId}/is-exported")
    public ResponseEntity<Boolean> checkIndividualExported(
            @PathVariable UUID treeId,
            @PathVariable UUID individualId,
            Authentication authentication) {

        log.info("Checking if individual {} is exported, requested by: {}",
                individualId, authentication.getName());

        boolean isExported = treeCloneService.isIndividualAlreadyExported(individualId);
        return ResponseEntity.ok(isExported);
    }

    /**
     * Get clone information for an individual
     * Shows both trees this individual was cloned TO and source info if this is a clone
     * GET /api/trees/{treeId}/individuals/{individualId}/clone-info
     */
    @GetMapping("/{treeId}/individuals/{individualId}/clone-info")
    public ResponseEntity<IndividualCloneInfoResponse> getIndividualCloneInfo(
            @PathVariable UUID treeId,
            @PathVariable UUID individualId,
            Authentication authentication) {

        log.info("Getting clone info for individual {} in tree {}, requested by: {}",
                individualId, treeId, authentication.getName());

        IndividualCloneInfoResponse response = treeCloneService.getIndividualCloneInfo(treeId, individualId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get clone information for a tree
     * Shows if this tree is a clone or has been cloned, and provides navigation to related trees
     * GET /api/trees/{treeId}/clone-info
     */
    @GetMapping("/{treeId}/clone-info")
    public ResponseEntity<TreeCloneInfoResponse> getTreeCloneInfo(
            @PathVariable UUID treeId,
            Authentication authentication) {

        log.info("Getting clone info for tree {}, requested by: {}",
                treeId, authentication.getName());

        TreeCloneInfoResponse response = treeCloneService.getTreeCloneInfo(treeId);
        return ResponseEntity.ok(response);
    }

    /**
     * Add a tree admin
     * Only the owner can add tree admins
     * POST /api/trees/{id}/admins/{adminUserId}
     */
    @PostMapping("/{id}/admins/{adminUserId}")
    public ResponseEntity<TreeResponse> addTreeAdmin(
            @PathVariable UUID id,
            @PathVariable UUID adminUserId,
            Authentication authentication) {

        log.info("Adding tree admin for tree {} user {} by user: {}",
                id, adminUserId, authentication.getName());
        TreeResponse response = treeService.addTreeAdmin(id, adminUserId, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Remove a tree admin
     * Only the owner can remove tree admins
     * DELETE /api/trees/{id}/admins/{adminUserId}
     */
    @DeleteMapping("/{id}/admins/{adminUserId}")
    public ResponseEntity<TreeResponse> removeTreeAdmin(
            @PathVariable UUID id,
            @PathVariable UUID adminUserId,
            Authentication authentication) {

        log.info("Removing tree admin {} from tree {} by user: {}", adminUserId, id, authentication.getName());
        TreeResponse response = treeService.removeTreeAdmin(id, adminUserId, authentication.getName());
        return ResponseEntity.ok(response);
    }
}
