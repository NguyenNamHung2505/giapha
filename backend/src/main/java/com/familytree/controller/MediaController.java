package com.familytree.controller;

import com.familytree.dto.media.MediaResponse;
import com.familytree.dto.media.UpdateMediaRequest;
import com.familytree.model.Media;
import com.familytree.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;
import java.util.UUID;

/**
 * REST controller for media management
 */
@RestController
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class MediaController {

    private final MediaService mediaService;

    /**
     * Upload media file for an individual
     * POST /api/individuals/{individualId}/media
     */
    @PostMapping("/api/individuals/{individualId}/media")
    public ResponseEntity<MediaResponse> uploadMedia(
            @PathVariable UUID individualId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "caption", required = false) String caption,
            Authentication authentication) {

        log.info("Uploading media for individual {} by user: {}", individualId, authentication.getName());
        
        MediaResponse response = mediaService.uploadMedia(
                individualId,
                file,
                caption,
                authentication.getName()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * List all media for an individual
     * GET /api/individuals/{individualId}/media
     */
    @GetMapping("/api/individuals/{individualId}/media")
    public ResponseEntity<List<MediaResponse>> listMedia(
            @PathVariable UUID individualId,
            Authentication authentication) {

        log.info("Listing media for individual {} by user: {}", individualId, authentication.getName());
        
        List<MediaResponse> mediaList = mediaService.listMediaForIndividual(
                individualId,
                authentication.getName()
        );
        
        return ResponseEntity.ok(mediaList);
    }

    /**
     * Get media by ID (metadata only)
     * GET /api/media/{id}
     */
    @GetMapping("/api/media/{id}")
    public ResponseEntity<MediaResponse> getMedia(
            @PathVariable UUID id,
            Authentication authentication) {

        log.info("Fetching media {} by user: {}", id, authentication.getName());
        
        MediaResponse response = mediaService.getMedia(id, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Update media metadata (caption)
     * PUT /api/media/{id}
     */
    @PutMapping("/api/media/{id}")
    public ResponseEntity<MediaResponse> updateMedia(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMediaRequest request,
            Authentication authentication) {

        log.info("Updating media {} by user: {}", id, authentication.getName());
        
        MediaResponse response = mediaService.updateMedia(id, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Delete media
     * DELETE /api/media/{id}
     */
    @DeleteMapping("/api/media/{id}")
    public ResponseEntity<Void> deleteMedia(
            @PathVariable UUID id,
            Authentication authentication) {

        log.info("Deleting media {} by user: {}", id, authentication.getName());
        
        mediaService.deleteMedia(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    /**
     * Download media file
     * GET /api/media/{id}/download
     */
    @GetMapping("/api/media/{id}/download")
    public ResponseEntity<InputStreamResource> downloadMedia(
            @PathVariable UUID id,
            Authentication authentication) {

        log.info("Downloading media {} by user: {}", id, authentication.getName());

        // Get media metadata
        Media media = mediaService.getMediaEntity(id, authentication.getName());
        
        // Get file stream
        InputStream inputStream = mediaService.downloadMedia(id, authentication.getName());
        InputStreamResource resource = new InputStreamResource(inputStream);

        // Set headers for file download
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, 
                "attachment; filename=\"" + media.getFilename() + "\"");
        headers.add(HttpHeaders.CACHE_CONTROL, "max-age=3600");
        
        // Parse media type
        MediaType mediaType = parseMediaType(media.getMimeType());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .contentLength(media.getFileSize() != null ? media.getFileSize() : -1)
                .body(resource);
    }

    /**
     * Stream media file (for inline viewing)
     * GET /api/media/{id}/stream
     */
    @GetMapping("/api/media/{id}/stream")
    public ResponseEntity<InputStreamResource> streamMedia(
            @PathVariable UUID id,
            Authentication authentication) {

        log.info("Streaming media {} by user: {}", id, authentication.getName());

        // Get media metadata
        Media media = mediaService.getMediaEntity(id, authentication.getName());
        
        // Get file stream
        InputStream inputStream = mediaService.downloadMedia(id, authentication.getName());
        InputStreamResource resource = new InputStreamResource(inputStream);

        // Set headers for inline viewing
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline");
        headers.add(HttpHeaders.CACHE_CONTROL, "max-age=3600");
        
        // Parse media type
        MediaType mediaType = parseMediaType(media.getMimeType());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .contentLength(media.getFileSize() != null ? media.getFileSize() : -1)
                .body(resource);
    }

    /**
     * Download thumbnail
     * GET /api/media/{id}/thumbnail
     */
    @GetMapping("/api/media/{id}/thumbnail")
    public ResponseEntity<InputStreamResource> downloadThumbnail(
            @PathVariable UUID id,
            Authentication authentication) {

        log.info("Downloading thumbnail for media {} by user: {}", id, authentication.getName());

        try {
            InputStream inputStream = mediaService.downloadThumbnail(id, authentication.getName());
            InputStreamResource resource = new InputStreamResource(inputStream);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline");
            headers.add(HttpHeaders.CACHE_CONTROL, "max-age=7200"); // Cache thumbnails longer

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
                    
        } catch (IllegalArgumentException e) {
            // Not an image, return 404
            return ResponseEntity.notFound().build();
        }
    }

    // ========== Private Helper Methods ==========

    /**
     * Parse MIME type string to Spring MediaType
     */
    private MediaType parseMediaType(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(mimeType);
        } catch (Exception e) {
            log.warn("Invalid MIME type: {}, using default", mimeType);
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}

