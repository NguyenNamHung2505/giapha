package com.familytree.dto.individual;

import com.familytree.model.Gender;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for individual response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndividualResponse {

    private UUID id;
    private UUID treeId;
    private String treeName;
    private String givenName;
    private String middleName;
    private String surname;
    private String suffix;
    private String fullName;
    private Gender gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    private String birthPlace;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate deathDate;

    private String deathPlace;
    private String biography;
    private String notes;
    private String profilePictureUrl;
    private String facebookLink;
    private String phoneNumber;
    private int mediaCount;
    private int eventCount;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
