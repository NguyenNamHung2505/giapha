package com.familytree.repository;

import com.familytree.model.TreeInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for TreeInvitation entities
 */
@Repository
public interface TreeInvitationRepository extends JpaRepository<TreeInvitation, UUID> {

    /**
     * Find invitation by token
     */
    Optional<TreeInvitation> findByToken(String token);

    /**
     * Find all invitations for a tree
     */
    @Query("SELECT i FROM TreeInvitation i WHERE i.tree.id = :treeId ORDER BY i.createdAt DESC")
    List<TreeInvitation> findByTreeId(UUID treeId);

    /**
     * Find pending invitations for an email
     */
    @Query("SELECT i FROM TreeInvitation i WHERE i.inviteeEmail = :email " +
           "AND i.status = 'PENDING' AND i.expiresAt > :now ORDER BY i.createdAt DESC")
    List<TreeInvitation> findPendingByEmail(String email, LocalDateTime now);

    /**
     * Find pending invitations for a tree and email
     */
    @Query("SELECT i FROM TreeInvitation i WHERE i.tree.id = :treeId AND i.inviteeEmail = :email " +
           "AND i.status = 'PENDING' AND i.expiresAt > :now")
    Optional<TreeInvitation> findPendingByTreeAndEmail(UUID treeId, String email, LocalDateTime now);

    /**
     * Count pending invitations for a tree
     */
    @Query("SELECT COUNT(i) FROM TreeInvitation i WHERE i.tree.id = :treeId " +
           "AND i.status = 'PENDING' AND i.expiresAt > :now")
    long countPendingByTreeId(UUID treeId, LocalDateTime now);

    /**
     * Delete expired invitations
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM TreeInvitation i WHERE i.expiresAt < :now AND i.status = 'PENDING'")
    void deleteExpired(LocalDateTime now);

    /**
     * Delete all invitations for a tree
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM TreeInvitation i WHERE i.tree.id = :treeId")
    void deleteByTreeId(UUID treeId);
}
