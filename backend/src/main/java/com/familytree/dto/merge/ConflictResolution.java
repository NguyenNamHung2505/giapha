package com.familytree.dto.merge;

/**
 * Resolution strategies for merge conflicts
 */
public enum ConflictResolution {
    /**
     * Use data from source tree (overwrite target)
     */
    THEIRS,
    
    /**
     * Keep data from target tree (ignore source changes)
     */
    OURS,
    
    /**
     * Manual resolution - user picks field by field
     */
    MANUAL,
    
    /**
     * Auto-merge - combine non-conflicting data, use newer dates
     */
    AUTO_MERGE
}
