package com.familytree.dto.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating media metadata
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMediaRequest {

    private String caption;
}
