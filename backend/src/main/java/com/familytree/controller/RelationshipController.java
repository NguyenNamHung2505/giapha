package com.familytree.controller;

import com.familytree.dto.relationship.CreateRelationshipRequest;
import com.familytree.dto.relationship.RelationshipResponse;
import com.familytree.dto.relationship.UpdateRelationshipRequest;
import com.familytree.service.RelationshipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for relationship management
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class RelationshipController {

    private final RelationshipService relationshipService;

    /**
     * Create a new relationship in a tree
     * POST /api/trees/{treeId}/relationships
     */
    @PostMapping("/api/trees/{treeId}/relationships")
    public ResponseEntity<RelationshipResponse> createRelationship(
            @PathVariable UUID treeId,
            @Valid @RequestBody CreateRelationshipRequest request,
            Authentication authentication) {

        log.info("Creating relationship in tree {} for user: {}", treeId, authentication.getName());
        RelationshipResponse response = relationshipService.createRelationship(treeId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all relationships in a tree
     * GET /api/trees/{treeId}/relationships
     */
    @GetMapping("/api/trees/{treeId}/relationships")
    public ResponseEntity<List<RelationshipResponse>> listRelationships(
            @PathVariable UUID treeId,
            Authentication authentication) {

        log.info("Listing relationships in tree {} for user: {}", treeId, authentication.getName());
        List<RelationshipResponse> relationships = relationshipService.listRelationships(treeId, authentication.getName());
        return ResponseEntity.ok(relationships);
    }

    /**
     * Get a specific relationship by ID
     * GET /api/relationships/{id}
     */
    @GetMapping("/api/relationships/{id}")
    public ResponseEntity<RelationshipResponse> getRelationship(
            @PathVariable UUID id,
            Authentication authentication) {

        log.info("Fetching relationship {} for user: {}", id, authentication.getName());
        RelationshipResponse response = relationshipService.getRelationship(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Get all relationships for a specific individual
     * GET /api/individuals/{individualId}/relationships
     */
    @GetMapping("/api/individuals/{individualId}/relationships")
    public ResponseEntity<List<RelationshipResponse>> getRelationshipsForIndividual(
            @PathVariable UUID individualId,
            Authentication authentication) {

        log.info("Fetching relationships for individual {} for user: {}", individualId, authentication.getName());
        List<RelationshipResponse> relationships = relationshipService.listRelationshipsForIndividual(individualId, authentication.getName());
        return ResponseEntity.ok(relationships);
    }

    /**
     * Get parents of an individual
     * GET /api/individuals/{individualId}/parents
     */
    @GetMapping("/api/individuals/{individualId}/parents")
    public ResponseEntity<List<RelationshipResponse>> getParents(
            @PathVariable UUID individualId,
            Authentication authentication) {

        log.info("Fetching parents for individual {} for user: {}", individualId, authentication.getName());
        List<RelationshipResponse> parents = relationshipService.getParents(individualId, authentication.getName());
        return ResponseEntity.ok(parents);
    }

    /**
     * Get children of an individual
     * GET /api/individuals/{individualId}/children
     */
    @GetMapping("/api/individuals/{individualId}/children")
    public ResponseEntity<List<RelationshipResponse>> getChildren(
            @PathVariable UUID individualId,
            Authentication authentication) {

        log.info("Fetching children for individual {} for user: {}", individualId, authentication.getName());
        List<RelationshipResponse> children = relationshipService.getChildren(individualId, authentication.getName());
        return ResponseEntity.ok(children);
    }

    /**
     * Get spouses/partners of an individual
     * GET /api/individuals/{individualId}/spouses
     */
    @GetMapping("/api/individuals/{individualId}/spouses")
    public ResponseEntity<List<RelationshipResponse>> getSpouses(
            @PathVariable UUID individualId,
            Authentication authentication) {

        log.info("Fetching spouses for individual {} for user: {}", individualId, authentication.getName());
        List<RelationshipResponse> spouses = relationshipService.getSpouses(individualId, authentication.getName());
        return ResponseEntity.ok(spouses);
    }

    /**
     * Update a relationship
     * PUT /api/relationships/{id}
     */
    @PutMapping("/api/relationships/{id}")
    public ResponseEntity<RelationshipResponse> updateRelationship(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRelationshipRequest request,
            Authentication authentication) {

        log.info("Updating relationship {} for user: {}", id, authentication.getName());
        RelationshipResponse response = relationshipService.updateRelationship(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a relationship
     * DELETE /api/relationships/{id}
     */
    @DeleteMapping("/api/relationships/{id}")
    public ResponseEntity<Void> deleteRelationship(
            @PathVariable UUID id,
            Authentication authentication) {

        log.info("Deleting relationship {} for user: {}", id, authentication.getName());
        relationshipService.deleteRelationship(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
