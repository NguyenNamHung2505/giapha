package com.familytree.dto.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request for searching individuals in a tree
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {

    private String query;
    private String gender;
    private Integer birthYearFrom;
    private Integer birthYearTo;
    private Integer deathYearFrom;
    private Integer deathYearTo;
    private String birthPlace;
    private String deathPlace;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}
