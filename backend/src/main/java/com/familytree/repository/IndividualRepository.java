package com.familytree.repository;

import com.familytree.model.FamilyTree;
import com.familytree.model.Individual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Individual entity
 */
@Repository
public interface IndividualRepository extends JpaRepository<Individual, UUID> {

    /**
     * Find all individuals in a specific tree
     * @param tree the family tree
     * @param pageable pagination information
     * @return page of individuals
     */
    Page<Individual> findByTree(FamilyTree tree, Pageable pageable);

    /**
     * Find all individuals in a specific tree with eager loading of tree
     * @param treeId the tree ID
     * @param pageable pagination information
     * @return page of individuals
     */
    @Query("SELECT i FROM Individual i JOIN FETCH i.tree WHERE i.tree.id = :treeId")
    List<Individual> findByTreeIdWithTree(@Param("treeId") UUID treeId);

    /**
     * Find all individuals in a specific tree (list version)
     * @param tree the family tree
     * @return list of individuals
     */
    List<Individual> findByTree(FamilyTree tree);

    /**
     * Find individuals by tree ID
     * @param treeId the tree ID
     * @return list of individuals
     */
    List<Individual> findByTreeId(UUID treeId);

    /**
     * Count individuals in a tree
     * @param treeId the tree ID
     * @return count of individuals
     */
    long countByTreeId(UUID treeId);

    /**
     * Search individuals by name within a tree
     * Searches in givenName, surname, and suffix fields
     * Uses unaccent to ignore Vietnamese diacritics
     * @param treeId the tree ID
     * @param searchTerm the search term
     * @param pageable pagination information
     * @return page of matching individuals
     */
    @Query(value = "SELECT * FROM individuals i WHERE i.tree_id = :treeId " +
           "AND (unaccent(LOWER(i.given_name)) LIKE unaccent(LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "OR unaccent(LOWER(i.surname)) LIKE unaccent(LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "OR unaccent(LOWER(i.suffix)) LIKE unaccent(LOWER(CONCAT('%', :searchTerm, '%'))))",
           countQuery = "SELECT count(*) FROM individuals i WHERE i.tree_id = :treeId " +
           "AND (unaccent(LOWER(i.given_name)) LIKE unaccent(LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "OR unaccent(LOWER(i.surname)) LIKE unaccent(LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "OR unaccent(LOWER(i.suffix)) LIKE unaccent(LOWER(CONCAT('%', :searchTerm, '%'))))",
           nativeQuery = true)
    Page<Individual> searchByName(@Param("treeId") UUID treeId,
                                   @Param("searchTerm") String searchTerm,
                                   Pageable pageable);

    /**
     * Count individuals in a tree
     * @param tree the family tree
     * @return count of individuals
     */
    long countByTree(FamilyTree tree);

    /**
     * Find individuals with no relationships (orphaned individuals)
     * @param treeId the tree ID
     * @return list of orphaned individuals
     */
    @Query("SELECT i FROM Individual i WHERE i.tree.id = :treeId " +
           "AND NOT EXISTS (SELECT r FROM Relationship r WHERE r.individual1.id = i.id OR r.individual2.id = i.id)")
    List<Individual> findOrphanedIndividuals(@Param("treeId") UUID treeId);

    /**
     * Find individual by ID with eager loading of tree
     * @param individualId the individual ID
     * @return individual with tree loaded
     */
    @Query("SELECT i FROM Individual i JOIN FETCH i.tree WHERE i.id = :individualId")
    java.util.Optional<Individual> findByIdWithTree(@Param("individualId") UUID individualId);

    /**
     * Delete all individuals in a tree
     * @param treeId the tree ID
     */
    void deleteByTreeId(UUID treeId);
}
