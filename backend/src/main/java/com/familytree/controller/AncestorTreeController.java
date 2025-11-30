package com.familytree.controller;

import com.familytree.dto.tree.AncestorTreeResponse;
import com.familytree.service.AncestorTreeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for ancestor tree visualization
 */
@RestController
@RequestMapping("/api/trees/{treeId}")
@RequiredArgsConstructor
@Slf4j
public class AncestorTreeController {

    private final AncestorTreeService ancestorTreeService;

    /**
     * Get ancestor tree for an individual
     * GET /api/trees/{treeId}/individuals/{individualId}/ancestors?generations=3
     *
     * @param treeId       The tree ID
     * @param individualId The individual to get ancestors for
     * @param generations  Number of generations to fetch (default 3, max 10)
     * @return AncestorTreeResponse containing the ancestor tree
     */
    @GetMapping("/individuals/{individualId}/ancestors")
    public ResponseEntity<AncestorTreeResponse> getAncestorTree(
            @PathVariable UUID treeId,
            @PathVariable UUID individualId,
            @RequestParam(defaultValue = "3") int generations,
            Authentication authentication) {

        log.info("Fetching ancestor tree for individual {} with {} generations in tree {}",
                individualId, generations, treeId);

        // Validate generations range
        if (generations < 1) generations = 1;
        if (generations > 10) generations = 10;

        AncestorTreeResponse response = ancestorTreeService.getAncestorTree(
                individualId,
                generations,
                authentication.getName()
        );

        return ResponseEntity.ok(response);
    }
}
