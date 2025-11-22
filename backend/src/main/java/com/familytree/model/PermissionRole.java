package com.familytree.model;

/**
 * Permission roles for tree access control
 */
public enum PermissionRole {
    OWNER,   // Full control, can delete tree, manage permissions
    EDITOR,  // Can add/edit/delete individuals and relationships
    VIEWER   // Read-only access
}
