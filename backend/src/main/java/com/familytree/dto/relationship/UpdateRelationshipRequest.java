package com.familytree.dto.relationship;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request DTO for updating an existing relationship
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRelationshipRequest {

    private LocalDate startDate;

    private LocalDate endDate;
}
