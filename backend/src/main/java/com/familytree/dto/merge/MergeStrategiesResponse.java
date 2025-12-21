package com.familytree.dto.merge;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for available merge strategies
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeStrategiesResponse {

    private List<StrategyInfo> strategies;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StrategyInfo {
        private MergeStrategy strategy;
        private String name;
        private String description;
        private boolean requiresCloneRelation;
    }
}
