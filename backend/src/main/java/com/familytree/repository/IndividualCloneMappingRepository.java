package com.familytree.repository;

import com.familytree.model.IndividualCloneMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for IndividualCloneMapping entity
 */
@Repository
public interface IndividualCloneMappingRepository extends JpaRepository<IndividualCloneMapping, UUID> {

    /**
     * Find all clones of a source individual
     * @param sourceIndividualId the source individual ID
     * @return list of clone mappings
     */
    List<IndividualCloneMapping> findBySourceIndividualId(UUID sourceIndividualId);

    /**
     * Find the source mapping for a cloned individual
     * @param clonedIndividualId the cloned individual ID
     * @return optional clone mapping
     */
    Optional<IndividualCloneMapping> findByClonedIndividualId(UUID clonedIndividualId);

    /**
     * Find all mappings for a source tree
     * @param sourceTreeId the source tree ID
     * @return list of clone mappings
     */
    List<IndividualCloneMapping> findBySourceTreeId(UUID sourceTreeId);

    /**
     * Find all mappings for a cloned tree
     * @param clonedTreeId the cloned tree ID
     * @return list of clone mappings
     */
    List<IndividualCloneMapping> findByClonedTreeId(UUID clonedTreeId);

    /**
     * Find mapping between specific source and cloned individuals
     * @param sourceIndividualId source individual ID
     * @param clonedIndividualId cloned individual ID
     * @return optional clone mapping
     */
    Optional<IndividualCloneMapping> findBySourceIndividualIdAndClonedIndividualId(
            UUID sourceIndividualId, UUID clonedIndividualId);

    /**
     * Find all trees that were cloned from a specific individual (root mappings only)
     * @param sourceIndividualId the source individual ID
     * @return list of root clone mappings
     */
    @Query("SELECT m FROM IndividualCloneMapping m WHERE m.sourceIndividual.id = :sourceIndividualId AND m.rootIndividual = true")
    List<IndividualCloneMapping> findRootClonesBySourceIndividualId(@Param("sourceIndividualId") UUID sourceIndividualId);

    /**
     * Find the corresponding cloned individual in a specific cloned tree
     * @param sourceIndividualId the source individual ID
     * @param clonedTreeId the cloned tree ID
     * @return optional clone mapping
     */
    @Query("SELECT m FROM IndividualCloneMapping m WHERE m.sourceIndividual.id = :sourceIndividualId AND m.clonedTree.id = :clonedTreeId")
    Optional<IndividualCloneMapping> findBySourceIndividualIdAndClonedTreeId(
            @Param("sourceIndividualId") UUID sourceIndividualId,
            @Param("clonedTreeId") UUID clonedTreeId);

    /**
     * Check if a source individual has been cloned
     * @param sourceIndividualId the source individual ID
     * @return true if clones exist
     */
    boolean existsBySourceIndividualId(UUID sourceIndividualId);

    /**
     * Delete all mappings for a cloned tree (when tree is deleted)
     * @param clonedTreeId the cloned tree ID
     */
    void deleteByClonedTreeId(UUID clonedTreeId);

    /**
     * Delete all mappings for a source tree (when tree is deleted)
     * @param sourceTreeId the source tree ID
     */
    void deleteBySourceTreeId(UUID sourceTreeId);

    /**
     * Delete all mappings where individual is the source
     * @param sourceIndividualId the source individual ID
     */
    void deleteBySourceIndividualId(UUID sourceIndividualId);

    /**
     * Delete all mappings where individual is the clone
     * @param clonedIndividualId the cloned individual ID
     */
    void deleteByClonedIndividualId(UUID clonedIndividualId);
}
