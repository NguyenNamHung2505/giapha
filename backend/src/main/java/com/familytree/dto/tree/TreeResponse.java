package com.familytree.dto.tree;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DTO for family tree response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreeResponse {

    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private String ownerName;
    private String ownerEmail;

    // Tree Admins information (multiple admins supported)
    @Builder.Default
    private List<AdminInfo> admins = new ArrayList<>();

    private int individualsCount;
    private int relationshipsCount;

    // Root individual for default perspective when viewing
    private UUID rootIndividualId;
    private String rootIndividualName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Nested DTO for admin information
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminInfo {
        private UUID id;
        private String name;
        private String email;
    }
}
