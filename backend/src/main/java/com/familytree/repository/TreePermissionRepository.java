package com.familytree.repository;

import com.familytree.model.FamilyTree;
import com.familytree.model.PermissionRole;
import com.familytree.model.TreePermission;
import com.familytree.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for TreePermission entity
 */
@Repository
public interface TreePermissionRepository extends JpaRepository<TreePermission, UUID> {

    /**
     * Find all permissions for a specific tree
     * @param tree the family tree
     * @return list of permissions
     */
    List<TreePermission> findByTree(FamilyTree tree);

    /**
     * Find all permissions for a specific user
     * @param user the user
     * @return list of permissions
     */
    List<TreePermission> findByUser(User user);

    /**
     * Find permission for a specific user and tree
     * @param tree the family tree
     * @param user the user
     * @return Optional containing the permission if found
     */
    Optional<TreePermission> findByTreeAndUser(FamilyTree tree, User user);

    /**
     * Find permission by tree ID and user ID
     * @param treeId the tree ID
     * @param userId the user ID
     * @return Optional containing the permission if found
     */
    Optional<TreePermission> findByTreeIdAndUserId(UUID treeId, UUID userId);

    /**
     * Check if user has permission for a tree
     * @param treeId the tree ID
     * @param userId the user ID
     * @return true if user has permission
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM TreePermission p " +
           "WHERE p.tree.id = :treeId AND p.user.id = :userId")
    boolean hasPermission(@Param("treeId") UUID treeId, @Param("userId") UUID userId);

    /**
     * Check if user has specific role for a tree
     * @param treeId the tree ID
     * @param userId the user ID
     * @param role the required role
     * @return true if user has the specified role
     */
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM TreePermission p " +
           "WHERE p.tree.id = :treeId AND p.user.id = :userId AND p.role = :role")
    boolean hasRole(@Param("treeId") UUID treeId,
                    @Param("userId") UUID userId,
                    @Param("role") PermissionRole role);

    /**
     * Find all trees where user has a specific role
     * @param userId the user ID
     * @param role the permission role
     * @return list of permissions
     */
    @Query("SELECT p FROM TreePermission p WHERE p.user.id = :userId AND p.role = :role")
    List<TreePermission> findByUserIdAndRole(@Param("userId") UUID userId,
                                               @Param("role") PermissionRole role);

    /**
     * Count permissions for a tree (number of collaborators)
     * @param tree the family tree
     * @return count of permissions
     */
    long countByTree(FamilyTree tree);

    /**
     * Delete permission by tree and user
     * @param tree the family tree
     * @param user the user
     */
    void deleteByTreeAndUser(FamilyTree tree, User user);
}
