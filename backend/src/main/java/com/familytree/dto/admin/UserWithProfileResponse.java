package com.familytree.dto.admin;

import com.familytree.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user with their tree profile mapping
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserWithProfileResponse {

    private UUID id;
    private String username;
    private String email;
    private String name;
    private boolean admin;
    private boolean enabled;
    private LocalDateTime createdAt;
    private IndividualInfo linkedIndividual;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IndividualInfo {
        private UUID id;
        private String fullName;
        private String gender;
        private LocalDate birthDate;
        private String profilePictureUrl;
    }

    public static UserWithProfileResponse fromUser(User user) {
        return UserWithProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .admin(user.isAdmin())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .linkedIndividual(null)
                .build();
    }
}
