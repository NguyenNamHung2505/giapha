package com.familytree.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * FamilyTree entity representing a family tree
 */
@Entity
@Table(name = "family_trees", indexes = {
    @Index(name = "idx_tree_owner", columnList = "owner_id"),
    @Index(name = "idx_tree_created", columnList = "created_at"),
    @Index(name = "idx_tree_source_individual", columnList = "source_individual_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"owner", "admins", "individuals", "permissions", "relationships"})
@EqualsAndHashCode(exclude = {"owner", "admins", "individuals", "permissions", "relationships"})
public class FamilyTree {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnoreProperties({"ownedTrees", "treePermissions", "passwordHash"})
    private User owner;

    /**
     * Tree Admins - users with edit permissions on this tree but are not the owner.
     * When a tree is cloned, the cloner becomes an admin of the new tree.
     * Admins can edit individuals, relationships, and media but cannot:
     * - Delete the tree
     * - Manage collaborators/permissions
     * - Transfer ownership
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "tree_admins",
        joinColumns = @JoinColumn(name = "tree_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    @JsonIgnoreProperties({"ownedTrees", "treePermissions", "passwordHash"})
    private Set<User> admins = new HashSet<>();

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Clone tracking fields
    @Column(name = "source_tree_id")
    private UUID sourceTreeId;  // Original tree ID if this tree was cloned

    @Column(name = "source_individual_id")
    private UUID sourceIndividualId;  // Root individual ID from source tree

    @Column(name = "cloned_at")
    private LocalDateTime clonedAt;  // When this tree was cloned

    // Root individual for this tree (default perspective when viewing)
    // For cloned trees, this is the cloned root person
    @Column(name = "root_individual_id")
    private UUID rootIndividualId;

    // Relationships
    @OneToMany(mappedBy = "tree", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties({"tree", "mediaFiles", "events"})
    private Set<Individual> individuals = new HashSet<>();

    @OneToMany(mappedBy = "tree", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties({"tree", "user"})
    private Set<TreePermission> permissions = new HashSet<>();

    @OneToMany(mappedBy = "tree", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties({"tree"})
    private Set<Relationship> relationships = new HashSet<>();
}
