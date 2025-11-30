package com.familytree.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user tree profile mapping
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTreeProfileResponse {

    private UUID id;
    private UUID treeId;
    private String treeName;
    private IndividualInfo individual;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndividualInfo {
        private UUID id;
        private String givenName;
        private String surname;
        private String fullName;
        private String gender;
        private LocalDate birthDate;
        private String profilePictureUrl;
    }
}
