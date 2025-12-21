package com.familytree.controller;

import com.familytree.dto.merge.*;
import com.familytree.service.TreeMergeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

/**
 * REST Controller for tree merge operations (Git-like merge)
 */
@RestController
@RequestMapping("/api/trees/{targetTreeId}/merge")
@RequiredArgsConstructor
@Slf4j
public class TreeMergeController {

    private final TreeMergeService mergeService;

    /**
     * Preview a merge operation without making changes
     */
    @PostMapping("/preview")
    public ResponseEntity<MergePreviewResponse> previewMerge(
            @PathVariable UUID targetTreeId,
            @Valid @RequestBody TreeMergeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Preview merge request: source={}, target={}, strategy={}", 
                request.getSourceTreeId(), targetTreeId, request.getStrategy());
        
        MergePreviewResponse preview = mergeService.previewMerge(targetTreeId, request, userDetails.getUsername());
        return ResponseEntity.ok(preview);
    }

    /**
     * Execute a merge operation
     */
    @PostMapping("/execute")
    public ResponseEntity<MergeResultResponse> executeMerge(
            @PathVariable UUID targetTreeId,
            @Valid @RequestBody TreeMergeRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("Execute merge request: source={}, target={}, strategy={}", 
                request.getSourceTreeId(), targetTreeId, request.getStrategy());
        
        MergeResultResponse result = mergeService.executeMerge(targetTreeId, request, userDetails.getUsername());
        return ResponseEntity.ok(result);
    }

    /**
     * Get available merge strategies
     */
    @GetMapping("/strategies")
    public ResponseEntity<MergeStrategiesResponse> getMergeStrategies() {
        MergeStrategiesResponse response = MergeStrategiesResponse.builder()
                .strategies(java.util.List.of(
                        MergeStrategiesResponse.StrategyInfo.builder()
                                .strategy(MergeStrategy.IMPORT)
                                .name("Gộp cây")
                                .description("Gộp tất cả người từ cây nguồn vào cây đích.")
                                .requiresCloneRelation(false)
                                .build()
                ))
                .build();
        
        return ResponseEntity.ok(response);
    }
}
