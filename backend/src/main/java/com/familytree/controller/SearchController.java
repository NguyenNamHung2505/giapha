package com.familytree.controller;

import com.familytree.dto.search.SearchRequest;
import com.familytree.dto.search.SearchResult;
import com.familytree.security.UserPrincipal;
import com.familytree.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for search operations
 */
@RestController
@RequestMapping("/api/search")
@Slf4j
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    /**
     * Search individuals in a specific tree
     * GET /api/search/trees/{treeId}
     */
    @GetMapping("/trees/{treeId}")
    public ResponseEntity<Page<SearchResult>> searchInTree(
            @PathVariable String treeId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer birthYearFrom,
            @RequestParam(required = false) Integer birthYearTo,
            @RequestParam(required = false) Integer deathYearFrom,
            @RequestParam(required = false) Integer deathYearTo,
            @RequestParam(required = false) String birthPlace,
            @RequestParam(required = false) String deathPlace,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            UUID treeUuid = UUID.fromString(treeId);
            UUID userId = userPrincipal.getId();

            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .gender(gender)
                    .birthYearFrom(birthYearFrom)
                    .birthYearTo(birthYearTo)
                    .deathYearFrom(deathYearFrom)
                    .deathYearTo(deathYearTo)
                    .birthPlace(birthPlace)
                    .deathPlace(deathPlace)
                    .page(page)
                    .size(size)
                    .build();

            Page<SearchResult> results = searchService.searchInTree(treeUuid, userId, request);

            return ResponseEntity.ok(results);

        } catch (RuntimeException e) {
            log.error("Error searching tree: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Global search across all accessible trees
     * GET /api/search/global
     */
    @GetMapping("/global")
    public ResponseEntity<List<SearchResult>> globalSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int maxResults,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            UUID userId = userPrincipal.getId();

            List<SearchResult> results = searchService.globalSearch(userId, query, maxResults);

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("Error in global search: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
