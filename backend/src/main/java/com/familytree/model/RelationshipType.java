package com.familytree.model;

/**
 * Types of relationships between individuals
 */
public enum RelationshipType {
    SPOUSE,
    PARTNER,
    PARENT_CHILD,  // individual1 is parent of individual2 (deprecated - use MOTHER_CHILD or FATHER_CHILD)
    MOTHER_CHILD,  // individual1 is mother of individual2
    FATHER_CHILD,  // individual1 is father of individual2
    SIBLING,
    ADOPTED_PARENT_CHILD,
    STEP_PARENT_CHILD
}
