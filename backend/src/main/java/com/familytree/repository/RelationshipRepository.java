package com.familytree.repository;

import com.familytree.model.Individual;
import com.familytree.model.Relationship;
import com.familytree.model.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Relationship entity
 */
@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, UUID> {

    /**
     * Find all relationships for a specific individual
     * @param individualId the individual ID
     * @return list of relationships
     */
    @Query("SELECT r FROM Relationship r WHERE r.individual1.id = :individualId OR r.individual2.id = :individualId")
    List<Relationship> findByIndividual(@Param("individualId") UUID individualId);

    /**
     * Find all relationships in a tree
     * @param treeId the tree ID
     * @return list of relationships
     */
    List<Relationship> findByTreeId(UUID treeId);

    /**
     * Find all relationships in a tree with individuals eagerly loaded
     * @param treeId the tree ID
     * @return list of relationships with individuals
     */
    @Query("SELECT r FROM Relationship r " +
           "LEFT JOIN FETCH r.individual1 " +
           "LEFT JOIN FETCH r.individual2 " +
           "WHERE r.tree.id = :treeId")
    List<Relationship> findByTreeIdWithIndividuals(@Param("treeId") UUID treeId);

    /**
     * Count relationships in a tree
     * @param treeId the tree ID
     * @return count of relationships
     */
    long countByTreeId(UUID treeId);

    /**
     * Find relationships of a specific type for an individual
     * @param individualId the individual ID
     * @param type the relationship type
     * @return list of relationships
     */
    @Query("SELECT r FROM Relationship r WHERE (r.individual1.id = :individualId OR r.individual2.id = :individualId) " +
           "AND r.type = :type")
    List<Relationship> findByIndividualAndType(@Param("individualId") UUID individualId,
                                                 @Param("type") RelationshipType type);

    /**
     * Find parents of an individual (where individual is child in PARENT_CHILD relationship)
     * @param individualId the individual ID (child)
     * @return list of parent relationships
     */
    @Query("SELECT r FROM Relationship r WHERE r.individual2.id = :individualId " +
           "AND r.type IN ('PARENT_CHILD', 'MOTHER_CHILD', 'FATHER_CHILD', 'ADOPTED_PARENT_CHILD', 'STEP_PARENT_CHILD')")
    List<Relationship> findParents(@Param("individualId") UUID individualId);

    /**
     * Find children of an individual (where individual is parent in PARENT_CHILD relationship)
     * @param individualId the individual ID (parent)
     * @return list of child relationships
     */
    @Query("SELECT r FROM Relationship r WHERE r.individual1.id = :individualId " +
           "AND r.type IN ('PARENT_CHILD', 'MOTHER_CHILD', 'FATHER_CHILD', 'ADOPTED_PARENT_CHILD', 'STEP_PARENT_CHILD')")
    List<Relationship> findChildren(@Param("individualId") UUID individualId);

    /**
     * Find spouses/partners of an individual
     * @param individualId the individual ID
     * @return list of spouse/partner relationships
     */
    @Query("SELECT r FROM Relationship r WHERE (r.individual1.id = :individualId OR r.individual2.id = :individualId) " +
           "AND r.type IN ('SPOUSE', 'PARTNER')")
    List<Relationship> findSpouses(@Param("individualId") UUID individualId);

    /**
     * Check if a relationship already exists between two individuals
     * @param ind1Id first individual ID
     * @param ind2Id second individual ID
     * @param type relationship type
     * @return true if relationship exists
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Relationship r " +
           "WHERE ((r.individual1.id = :ind1Id AND r.individual2.id = :ind2Id) " +
           "OR (r.individual1.id = :ind2Id AND r.individual2.id = :ind1Id)) " +
           "AND r.type = :type")
    boolean existsRelationship(@Param("ind1Id") UUID ind1Id,
                                @Param("ind2Id") UUID ind2Id,
                                @Param("type") RelationshipType type);

    /**
     * Find all ancestors using recursive CTE (PostgreSQL specific)
     * This will be implemented in the service layer using native query
     */
    // Native query will be added in service layer for better control

    /**
     * Delete all relationships in a tree
     * @param treeId the tree ID
     */
    void deleteByTreeId(UUID treeId);

    /**
     * Delete all relationships involving an individual
     * @param individualId the individual ID
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Relationship r WHERE r.individual1.id = :individualId OR r.individual2.id = :individualId")
    void deleteByIndividualId(@Param("individualId") UUID individualId);

    /**
     * Check if a relationship exists between two individuals in a tree
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Relationship r " +
           "WHERE r.tree.id = :treeId " +
           "AND ((r.individual1.id = :ind1Id AND r.individual2.id = :ind2Id) " +
           "OR (r.individual1.id = :ind2Id AND r.individual2.id = :ind1Id)) " +
           "AND r.type = :type")
    boolean existsByTreeIdAndIndividualsAndType(@Param("treeId") UUID treeId,
                                                  @Param("ind1Id") UUID ind1Id,
                                                  @Param("ind2Id") UUID ind2Id,
                                                  @Param("type") RelationshipType type);
}
