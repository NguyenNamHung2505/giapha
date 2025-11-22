package com.familytree.controller;

import com.familytree.dto.individual.CreateIndividualRequest;
import com.familytree.dto.individual.IndividualResponse;
import com.familytree.dto.individual.UpdateIndividualRequest;
import com.familytree.service.IndividualService;
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
 * REST controller for individual management
 */
@RestController
@RequestMapping("/api/trees/{treeId}/individuals")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class IndividualController {

    private final IndividualService individualService;

    /**
     * Create a new individual in a tree
     * POST /api/trees/{treeId}/individuals
     */
    @PostMapping
    public ResponseEntity<IndividualResponse> createIndividual(
            @PathVariable UUID treeId,
            @Valid @RequestBody CreateIndividualRequest request,
            Authentication authentication) {

        log.info("Creating individual in tree {} for user: {}", treeId, authentication.getName());
        IndividualResponse response = individualService.createIndividual(treeId, request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all individuals in a tree
     * GET /api/trees/{treeId}/individuals
     */
    @GetMapping
    public ResponseEntity<Page<IndividualResponse>> listIndividuals(
            @PathVariable UUID treeId,
            Authentication authentication,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "surname", direction = Sort.Direction.ASC) Pageable pageable) {

        log.info("Listing individuals in tree {} for user: {}", treeId, authentication.getName());

        Page<IndividualResponse> individuals;
        if (search != null && !search.trim().isEmpty()) {
            individuals = individualService.searchIndividuals(treeId, search, authentication.getName(), pageable);
        } else {
            individuals = individualService.listIndividuals(treeId, authentication.getName(), pageable);
        }

        return ResponseEntity.ok(individuals);
    }

    /**
     * Get a specific individual by ID
     * GET /api/individuals/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<IndividualResponse> getIndividual(
            @PathVariable UUID treeId,
            @PathVariable UUID id,
            Authentication authentication) {

        log.info("Fetching individual {} for user: {}", id, authentication.getName());
        IndividualResponse response = individualService.getIndividual(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Update an individual
     * PUT /api/individuals/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<IndividualResponse> updateIndividual(
            @PathVariable UUID treeId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateIndividualRequest request,
            Authentication authentication) {

        log.info("Updating individual {} for user: {}", id, authentication.getName());
        IndividualResponse response = individualService.updateIndividual(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete an individual
     * DELETE /api/individuals/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIndividual(
            @PathVariable UUID treeId,
            @PathVariable UUID id,
            Authentication authentication) {

        log.info("Deleting individual {} for user: {}", id, authentication.getName());
        individualService.deleteIndividual(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
