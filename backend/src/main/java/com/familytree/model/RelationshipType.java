package com.familytree.model;

/**
 * Types of relationships between individuals
 */
public enum RelationshipType {
    SPOUSE,
    PARTNER,
    PARENT_CHILD,  // individual1 is parent of individual2
    SIBLING,
    ADOPTED_PARENT_CHILD,
    STEP_PARENT_CHILD
}
