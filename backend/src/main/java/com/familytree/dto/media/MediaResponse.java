package com.familytree.dto.media;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.familytree.model.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for media information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaResponse {

    private UUID id;

    private UUID individualId;

    private MediaType type;

    private String filename;

    private String caption;

    private Long fileSize;

    private String mimeType;

    private String downloadUrl;

    private String thumbnailUrl;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;
}
