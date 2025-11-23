package com.familytree.service;

import com.familytree.dto.media.MediaResponse;
import com.familytree.dto.media.UpdateMediaRequest;
import com.familytree.exception.ResourceNotFoundException;
import com.familytree.exception.UnauthorizedException;
import com.familytree.model.Individual;
import com.familytree.model.Media;
import com.familytree.model.MediaType;
import com.familytree.repository.IndividualRepository;
import com.familytree.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for media management operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaRepository mediaRepository;
    private final IndividualRepository individualRepository;
    private final MinioService minioService;
    private final PermissionService permissionService;

    // Allowed image MIME types
    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/bmp"
    );

    // Allowed document MIME types
    private static final Set<String> DOCUMENT_MIME_TYPES = Set.of(
            "application/pdf", "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain"
    );

    // Maximum file size: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // Thumbnail size
    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 200;

    /**
     * Upload media file for an individual
     */
    @Transactional
    public MediaResponse uploadMedia(
            UUID individualId,
            MultipartFile file,
            String caption,
            String username
    ) {
        log.info("Uploading media for individual {} by user {}", individualId, username);

        // Validate file
        validateFile(file);

        // Get individual with tree eagerly loaded and check permissions
        Individual individual = individualRepository.findByIdWithTree(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with id: " + individualId));

        UUID treeId = individual.getTree().getId();
        if (!permissionService.hasEditPermission(treeId, username)) {
            throw new UnauthorizedException("No permission to upload media to this tree");
        }

        try {
            // Determine media type
            MediaType mediaType = determineMediaType(file.getContentType());

            // Generate object name
            String originalFilename = file.getOriginalFilename();
            String objectName = minioService.generateObjectName(treeId, individualId, originalFilename);

            // Upload original file
            String storagePath = minioService.uploadFile(file, objectName);

            // Generate and upload thumbnail for images
            String thumbnailPath = null;
            if (isImage(file.getContentType())) {
                thumbnailPath = generateAndUploadThumbnail(file, objectName);
            }

            // Create media entity
            Media media = Media.builder()
                    .individual(individual)
                    .type(mediaType)
                    .filename(originalFilename)
                    .storagePath(storagePath)
                    .caption(caption)
                    .fileSize(file.getSize())
                    .mimeType(file.getContentType())
                    .build();

            Media savedMedia = mediaRepository.save(media);
            log.info("Media uploaded successfully with id: {}", savedMedia.getId());

            return mapToResponse(savedMedia, thumbnailPath);

        } catch (Exception e) {
            log.error("Error uploading media for individual {}", individualId, e);
            throw new RuntimeException("Failed to upload media: " + e.getMessage(), e);
        }
    }

    /**
     * Get media by ID
     */
    @Transactional(readOnly = true)
    public MediaResponse getMedia(UUID mediaId, String username) {
        log.info("Fetching media {} for user {}", mediaId, username);

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        // Access individual within transaction to trigger lazy loading
        UUID treeId = media.getIndividual().getTree().getId();
        if (!permissionService.hasViewPermission(treeId, username)) {
            throw new UnauthorizedException("No permission to view this media");
        }

        String thumbnailPath = isImage(media.getMimeType()) ?
                minioService.generateThumbnailName(media.getStoragePath()) : null;

        return mapToResponse(media, thumbnailPath);
    }

    /**
     * List all media for an individual
     */
    @Transactional(readOnly = true)
    public List<MediaResponse> listMediaForIndividual(UUID individualId, String username) {
        log.info("Listing media for individual {} by user {}", individualId, username);

        // Get individual with tree eagerly loaded
        Individual individual = individualRepository.findByIdWithTree(individualId)
                .orElseThrow(() -> new ResourceNotFoundException("Individual not found with id: " + individualId));

        UUID treeId = individual.getTree().getId();
        if (!permissionService.hasViewPermission(treeId, username)) {
            throw new UnauthorizedException("No permission to view media in this tree");
        }

        // Use fetch join to eagerly load individual relationship
        List<Media> mediaList = mediaRepository.findByIndividualIdWithIndividual(individualId);

        return mediaList.stream()
                .map(media -> {
                    String thumbnailPath = isImage(media.getMimeType()) ?
                            minioService.generateThumbnailName(media.getStoragePath()) : null;
                    return mapToResponse(media, thumbnailPath);
                })
                .collect(Collectors.toList());
    }

    /**
     * Update media metadata (caption)
     */
    @Transactional
    public MediaResponse updateMedia(UUID mediaId, UpdateMediaRequest request, String username) {
        log.info("Updating media {} by user {}", mediaId, username);

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        UUID treeId = media.getIndividual().getTree().getId();
        if (!permissionService.hasEditPermission(treeId, username)) {
            throw new UnauthorizedException("No permission to update media in this tree");
        }

        media.setCaption(request.getCaption());
        Media updatedMedia = mediaRepository.save(media);

        String thumbnailPath = isImage(media.getMimeType()) ?
                minioService.generateThumbnailName(media.getStoragePath()) : null;

        return mapToResponse(updatedMedia, thumbnailPath);
    }

    /**
     * Delete media
     */
    @Transactional
    public void deleteMedia(UUID mediaId, String username) {
        log.info("Deleting media {} by user {}", mediaId, username);

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        UUID treeId = media.getIndividual().getTree().getId();
        if (!permissionService.hasEditPermission(treeId, username)) {
            throw new UnauthorizedException("No permission to delete media in this tree");
        }

        try {
            // Delete from MinIO
            minioService.deleteFile(media.getStoragePath());

            // Delete thumbnail if it's an image
            if (isImage(media.getMimeType())) {
                String thumbnailPath = minioService.generateThumbnailName(media.getStoragePath());
                if (minioService.fileExists(thumbnailPath)) {
                    minioService.deleteFile(thumbnailPath);
                }
            }

            // Delete from database
            mediaRepository.delete(media);
            log.info("Media deleted successfully: {}", mediaId);

        } catch (Exception e) {
            log.error("Error deleting media {}", mediaId, e);
            throw new RuntimeException("Failed to delete media: " + e.getMessage(), e);
        }
    }

    /**
     * Download media file
     */
    @Transactional(readOnly = true)
    public InputStream downloadMedia(UUID mediaId, String username) {
        log.info("Downloading media {} by user {}", mediaId, username);

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        // Access individual.tree within transaction
        UUID treeId = media.getIndividual().getTree().getId();
        if (!permissionService.hasViewPermission(treeId, username)) {
            throw new UnauthorizedException("No permission to download this media");
        }

        try {
            return minioService.downloadFile(media.getStoragePath());
        } catch (Exception e) {
            log.error("Error downloading media {}", mediaId, e);
            throw new RuntimeException("Failed to download media: " + e.getMessage(), e);
        }
    }

    /**
     * Download thumbnail
     */
    @Transactional(readOnly = true)
    public InputStream downloadThumbnail(UUID mediaId, String username) {
        log.info("Downloading thumbnail for media {} by user {}", mediaId, username);

        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        if (!isImage(media.getMimeType())) {
            throw new IllegalArgumentException("Thumbnails are only available for images");
        }

        // Access individual.tree within transaction
        UUID treeId = media.getIndividual().getTree().getId();
        if (!permissionService.hasViewPermission(treeId, username)) {
            throw new UnauthorizedException("No permission to view this media");
        }

        try {
            String thumbnailPath = minioService.generateThumbnailName(media.getStoragePath());
            return minioService.downloadFile(thumbnailPath);
        } catch (Exception e) {
            log.error("Error downloading thumbnail for media {}", mediaId, e);
            throw new RuntimeException("Failed to download thumbnail: " + e.getMessage(), e);
        }
    }

    /**
     * Get media metadata (for serving files)
     */
    @Transactional(readOnly = true)
    public Media getMediaEntity(UUID mediaId, String username) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Media not found with id: " + mediaId));

        // Access individual within transaction to trigger lazy loading
        UUID treeId = media.getIndividual().getTree().getId();
        if (!permissionService.hasViewPermission(treeId, username)) {
            throw new UnauthorizedException("No permission to view this media");
        }

        return media;
    }

    // ========== Private Helper Methods ==========

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("File size exceeds maximum allowed size of %d MB",
                            MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !isAllowedMimeType(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Allowed types: images (JPEG, PNG, GIF, WebP) and documents (PDF, DOC, DOCX, TXT)"
            );
        }
    }

    /**
     * Check if MIME type is allowed
     */
    private boolean isAllowedMimeType(String mimeType) {
        return IMAGE_MIME_TYPES.contains(mimeType.toLowerCase()) ||
                DOCUMENT_MIME_TYPES.contains(mimeType.toLowerCase());
    }

    /**
     * Check if file is an image
     */
    private boolean isImage(String mimeType) {
        return mimeType != null && IMAGE_MIME_TYPES.contains(mimeType.toLowerCase());
    }

    /**
     * Determine media type from MIME type
     */
    private MediaType determineMediaType(String mimeType) {
        if (mimeType == null) {
            return MediaType.OTHER;
        }

        String lowerMimeType = mimeType.toLowerCase();
        if (IMAGE_MIME_TYPES.contains(lowerMimeType)) {
            return MediaType.PHOTO;
        } else if (DOCUMENT_MIME_TYPES.contains(lowerMimeType)) {
            return MediaType.DOCUMENT;
        } else if (lowerMimeType.startsWith("video/")) {
            return MediaType.VIDEO;
        } else if (lowerMimeType.startsWith("audio/")) {
            return MediaType.AUDIO;
        }
        return MediaType.OTHER;
    }

    /**
     * Generate and upload thumbnail for image
     */
    private String generateAndUploadThumbnail(MultipartFile file, String originalObjectName) throws Exception {
        log.debug("Generating thumbnail for {}", originalObjectName);

        try (InputStream inputStream = file.getInputStream();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            // Generate thumbnail
            Thumbnails.of(inputStream)
                    .size(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT)
                    .keepAspectRatio(true)
                    .outputFormat("jpg")
                    .outputQuality(0.8)
                    .toOutputStream(outputStream);

            byte[] thumbnailBytes = outputStream.toByteArray();

            // Upload thumbnail
            String thumbnailObjectName = minioService.generateThumbnailName(originalObjectName);
            try (ByteArrayInputStream thumbnailInputStream = new ByteArrayInputStream(thumbnailBytes)) {
                minioService.uploadFile(
                        thumbnailInputStream,
                        thumbnailObjectName,
                        "image/jpeg",
                        thumbnailBytes.length
                );
            }

            log.debug("Thumbnail generated and uploaded: {}", thumbnailObjectName);
            return thumbnailObjectName;

        } catch (Exception e) {
            log.warn("Failed to generate thumbnail for {}: {}", originalObjectName, e.getMessage());
            // Don't fail the entire upload if thumbnail generation fails
            return null;
        }
    }

    /**
     * Map Media entity to MediaResponse DTO
     */
    private MediaResponse mapToResponse(Media media, String thumbnailPath) {
        return MediaResponse.builder()
                .id(media.getId())
                .individualId(media.getIndividual().getId())
                .type(media.getType())
                .filename(media.getFilename())
                .caption(media.getCaption())
                .fileSize(media.getFileSize())
                .mimeType(media.getMimeType())
                .downloadUrl("/api/media/" + media.getId() + "/download")
                .thumbnailUrl(thumbnailPath != null ? "/api/media/" + media.getId() + "/thumbnail" : null)
                .uploadedAt(media.getUploadedAt())
                .build();
    }
}

