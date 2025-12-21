package com.familytree.repository;

import com.familytree.model.UserTreeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserTreeProfile entity
 */
@Repository
public interface UserTreeProfileRepository extends JpaRepository<UserTreeProfile, UUID> {

    /**
     * Find a user's profile mapping for a specific tree
     */
    @Query("SELECT utp FROM UserTreeProfile utp WHERE utp.user.id = :userId AND utp.tree.id = :treeId")
    Optional<UserTreeProfile> findByUserIdAndTreeId(@Param("userId") UUID userId, @Param("treeId") UUID treeId);

    /**
     * Find by user email and tree ID
     */
    @Query("SELECT utp FROM UserTreeProfile utp WHERE utp.user.email = :email AND utp.tree.id = :treeId")
    Optional<UserTreeProfile> findByUserEmailAndTreeId(@Param("email") String email, @Param("treeId") UUID treeId);

    /**
     * Check if a mapping exists
     */
    @Query("SELECT CASE WHEN COUNT(utp) > 0 THEN true ELSE false END FROM UserTreeProfile utp " +
           "WHERE utp.user.id = :userId AND utp.tree.id = :treeId")
    boolean existsByUserIdAndTreeId(@Param("userId") UUID userId, @Param("treeId") UUID treeId);

    /**
     * Delete mapping for a user and tree
     */
    void deleteByUserIdAndTreeId(UUID userId, UUID treeId);

    /**
     * Find profile by individual ID (to check if individual already has a linked user)
     */
    @Query("SELECT utp FROM UserTreeProfile utp WHERE utp.individual.id = :individualId")
    Optional<UserTreeProfile> findByIndividualId(@Param("individualId") UUID individualId);

    /**
     * Find all profiles for a tree
     */
    @Query("SELECT utp FROM UserTreeProfile utp WHERE utp.tree.id = :treeId")
    List<UserTreeProfile> findByTreeId(@Param("treeId") UUID treeId);

    /**
     * Delete all profiles for a user
     */
    void deleteByUserId(UUID userId);

    /**
     * Delete all profiles for an individual
     */
    void deleteByIndividualId(UUID individualId);

    /**
     * Delete all profiles for a tree
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM UserTreeProfile utp WHERE utp.tree.id = :treeId")
    void deleteByTreeId(@Param("treeId") UUID treeId);
}
