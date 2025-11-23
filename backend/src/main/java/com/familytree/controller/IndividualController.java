package com.familytree.controller;

import com.familytree.dto.individual.CreateIndividualRequest;
import com.familytree.dto.individual.IndividualResponse;
import com.familytree.dto.individual.UpdateIndividualRequest;
import com.familytree.service.IndividualService;
import com.familytree.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final MinioService minioService;

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
        IndividualResponse response = individualService.getIndividual(treeId, id, authentication.getName());
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

    /**
     * Upload profile picture/avatar for an individual
     * POST /api/trees/{treeId}/individuals/{id}/avatar
     */
    @PostMapping("/{id}/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @PathVariable UUID treeId,
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {

        try {
            log.info("Uploading avatar for individual {} by user: {}", id, authentication.getName());

            // Validate file
            if (file.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "File is empty");
                return ResponseEntity.badRequest().body(error);
            }

            // Validate file type (images only)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Only image files are allowed");
                return ResponseEntity.badRequest().body(error);
            }

            // Validate file size (max 5MB)
            long maxSize = 5 * 1024 * 1024; // 5MB
            if (file.getSize() > maxSize) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "File size exceeds 5MB limit");
                return ResponseEntity.badRequest().body(error);
            }

            // Upload avatar and update individual
            String avatarUrl = individualService.uploadAvatar(id, file, authentication.getName());

            Map<String, String> response = new HashMap<>();
            response.put("profilePictureUrl", avatarUrl);
            response.put("message", "Avatar uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Invalid request: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            log.error("Error uploading avatar", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to upload avatar: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * Delete profile picture/avatar for an individual
     * DELETE /api/trees/{treeId}/individuals/{id}/avatar
     */
    @DeleteMapping("/{id}/avatar")
    public ResponseEntity<Void> deleteAvatar(
            @PathVariable UUID treeId,
            @PathVariable UUID id,
            Authentication authentication) {

        try {
            log.info("Deleting avatar for individual {} by user: {}", id, authentication.getName());
            individualService.deleteAvatar(id, authentication.getName());
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            log.error("Error deleting avatar", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get/stream profile picture/avatar for an individual
     * GET /api/trees/{treeId}/individuals/{id}/avatar
     */
    @GetMapping("/{id}/avatar")
    public ResponseEntity<InputStreamResource> getAvatar(
            @PathVariable UUID treeId,
            @PathVariable UUID id,
            Authentication authentication) {

        try {
            log.info("Fetching avatar for individual {} by user: {}", id, authentication.getName());

            // Get individual to check if avatar exists
            IndividualResponse individual = individualService.getIndividual(treeId, id, authentication.getName());

            if (individual.getProfilePictureUrl() == null || individual.getProfilePictureUrl().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Find avatar file in MinIO
            String prefix = "avatars/individuals/" + id + "/";
            List<String> files = minioService.listFiles(prefix);

            if (files.isEmpty()) {
                log.warn("No avatar file found for individual {}", id);
                return ResponseEntity.notFound().build();
            }

            // Get the first file (should only be one avatar)
            String objectName = files.get(0);
            InputStream inputStream = minioService.downloadFile(objectName);
            InputStreamResource resource = new InputStreamResource(inputStream);

            // Determine content type from file extension
            String contentType = "image/jpeg"; // default
            if (objectName.endsWith(".png")) {
                contentType = "image/png";
            } else if (objectName.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (objectName.endsWith(".webp")) {
                contentType = "image/webp";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline");
            headers.add(HttpHeaders.CACHE_CONTROL, "max-age=3600");

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);

        } catch (Exception e) {
            log.error("Error fetching avatar for individual {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
