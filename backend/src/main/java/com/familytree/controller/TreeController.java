package com.familytree.controller;

import com.familytree.dto.tree.CreateTreeRequest;
import com.familytree.dto.tree.TreeResponse;
import com.familytree.dto.tree.UpdateTreeRequest;
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
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class TreeController {

    private final TreeService treeService;

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
}
