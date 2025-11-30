package com.familytree.repository;

import com.familytree.model.FamilyTree;
import com.familytree.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for FamilyTree entity
 */
@Repository
public interface FamilyTreeRepository extends JpaRepository<FamilyTree, UUID> {

    /**
     * Find all trees owned by a specific user
     * @param owner the owner user
     * @param pageable pagination information
     * @return page of family trees
     */
    Page<FamilyTree> findByOwner(User owner, Pageable pageable);

    /**
     * Find all trees owned by a specific user (list version)
     * @param owner the owner user
     * @return list of family trees
     */
    List<FamilyTree> findByOwner(User owner);

    /**
     * Find all trees where user has permission (owner, shared, or linked via UserTreeProfile)
     * @param userId the user ID
     * @param pageable pagination information
     * @return page of family trees
     */
    @Query("SELECT DISTINCT t FROM FamilyTree t " +
           "LEFT JOIN t.permissions p " +
           "LEFT JOIN UserTreeProfile utp ON utp.tree.id = t.id AND utp.user.id = :userId " +
           "WHERE t.owner.id = :userId OR p.user.id = :userId OR utp.id IS NOT NULL " +
           "ORDER BY t.updatedAt DESC")
    Page<FamilyTree> findTreesAccessibleByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find all trees (for admin users)
     * @param pageable pagination information
     * @return page of all family trees
     */
    @Query("SELECT t FROM FamilyTree t ORDER BY t.updatedAt DESC")
    Page<FamilyTree> findAllTrees(Pageable pageable);

    /**
     * Count trees owned by a user
     * @param owner the owner user
     * @return count of trees
     */
    long countByOwner(User owner);

    /**
     * Check if a tree was already created from a specific individual
     * Used to validate before creating a new tree from an individual
     * @param sourceIndividualId the source individual ID
     * @return true if a tree already exists with this source individual
     */
    boolean existsBySourceIndividualId(UUID sourceIndividualId);

    /**
     * Find trees that were cloned from a specific source tree
     * @param sourceTreeId the source tree ID
     * @return list of cloned trees
     */
    List<FamilyTree> findBySourceTreeId(UUID sourceTreeId);
}
