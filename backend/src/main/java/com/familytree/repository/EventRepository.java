package com.familytree.repository;

import com.familytree.model.Event;
import com.familytree.model.EventType;
import com.familytree.model.Individual;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Repository interface for Event entity
 */
@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * Find all events for a specific individual
     * @param individual the individual
     * @return list of events ordered by date
     */
    List<Event> findByIndividualOrderByEventDateAsc(Individual individual);

    /**
     * Find events by individual ID
     * @param individualId the individual ID
     * @return list of events
     */
    List<Event> findByIndividualId(UUID individualId);

    /**
     * Find events by type for an individual
     * @param individual the individual
     * @param type the event type
     * @return list of events
     */
    List<Event> findByIndividualAndType(Individual individual, EventType type);

    /**
     * Find all events in a tree
     * @param treeId the tree ID
     * @param pageable pagination information
     * @return page of events
     */
    @Query("SELECT e FROM Event e WHERE e.individual.tree.id = :treeId ORDER BY e.eventDate DESC")
    Page<Event> findByTreeId(@Param("treeId") UUID treeId, Pageable pageable);

    /**
     * Find events within a date range for a tree
     * @param treeId the tree ID
     * @param startDate the start date
     * @param endDate the end date
     * @return list of events
     */
    @Query("SELECT e FROM Event e WHERE e.individual.tree.id = :treeId " +
           "AND e.eventDate BETWEEN :startDate AND :endDate " +
           "ORDER BY e.eventDate ASC")
    List<Event> findByTreeIdAndDateRange(@Param("treeId") UUID treeId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    /**
     * Count events for an individual
     * @param individual the individual
     * @return count of events
     */
    long countByIndividual(Individual individual);

    /**
     * Count events by individual ID
     * @param individualId the individual ID
     * @return count of events
     */
    long countByIndividualId(UUID individualId);

    /**
     * Find events by type across a tree
     * @param treeId the tree ID
     * @param type the event type
     * @return list of events
     */
    @Query("SELECT e FROM Event e WHERE e.individual.tree.id = :treeId AND e.type = :type " +
           "ORDER BY e.eventDate DESC")
    List<Event> findByTreeIdAndType(@Param("treeId") UUID treeId, @Param("type") EventType type);
}
