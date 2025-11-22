package com.familytree.repository;

import com.familytree.model.Individual;
import com.familytree.model.Media;
import com.familytree.model.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Media entity
 */
@Repository
public interface MediaRepository extends JpaRepository<Media, UUID> {

    /**
     * Find all media for a specific individual
     * @param individual the individual
     * @param pageable pagination information
     * @return page of media
     */
    Page<Media> findByIndividual(Individual individual, Pageable pageable);

    /**
     * Find all media for a specific individual (list version)
     * @param individual the individual
     * @return list of media
     */
    List<Media> findByIndividual(Individual individual);

    /**
     * Find media by individual ID
     * @param individualId the individual ID
     * @return list of media
     */
    List<Media> findByIndividualId(UUID individualId);

    /**
     * Find media by individual ID with eager loading of individual and tree
     * @param individualId the individual ID
     * @return list of media
     */
    @Query("SELECT DISTINCT m FROM Media m JOIN FETCH m.individual i JOIN FETCH i.tree WHERE i.id = :individualId")
    List<Media> findByIndividualIdWithIndividual(@Param("individualId") UUID individualId);

    /**
     * Find media by type for an individual
     * @param individual the individual
     * @param type the media type
     * @return list of media
     */
    List<Media> findByIndividualAndType(Individual individual, MediaType type);

    /**
     * Find all media in a tree
     * @param treeId the tree ID
     * @param pageable pagination information
     * @return page of media
     */
    @Query("SELECT m FROM Media m WHERE m.individual.tree.id = :treeId ORDER BY m.uploadedAt DESC")
    Page<Media> findByTreeId(@Param("treeId") UUID treeId, Pageable pageable);

    /**
     * Count media files for an individual
     * @param individual the individual
     * @return count of media
     */
    long countByIndividual(Individual individual);

    /**
     * Count media files by individual ID
     * @param individualId the individual ID
     * @return count of media
     */
    long countByIndividualId(UUID individualId);

    /**
     * Calculate total storage used by a tree
     * @param treeId the tree ID
     * @return total file size in bytes
     */
    @Query("SELECT COALESCE(SUM(m.fileSize), 0) FROM Media m WHERE m.individual.tree.id = :treeId")
    Long calculateTreeStorageSize(@Param("treeId") UUID treeId);
}
