package com.familytree.model;

import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity to store mapping between original individuals and their cloned copies
 * This enables navigation between source tree and cloned trees
 */
@Entity
@Table(name = "individual_clone_mappings",
        indexes = {
                @Index(name = "idx_clone_mapping_source", columnList = "source_individual_id"),
                @Index(name = "idx_clone_mapping_cloned", columnList = "cloned_individual_id"),
                @Index(name = "idx_clone_mapping_source_tree", columnList = "source_tree_id"),
                @Index(name = "idx_clone_mapping_cloned_tree", columnList = "cloned_tree_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_source_cloned_individual",
                        columnNames = {"source_individual_id", "cloned_individual_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndividualCloneMapping {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The original individual in the source tree
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_individual_id", nullable = false)
    private Individual sourceIndividual;

    /**
     * The cloned individual in the new tree
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloned_individual_id", nullable = false)
    private Individual clonedIndividual;

    /**
     * The source tree (for easier querying)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_tree_id", nullable = false)
    private FamilyTree sourceTree;

    /**
     * The cloned tree (for easier querying)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cloned_tree_id", nullable = false)
    private FamilyTree clonedTree;

    /**
     * Whether this individual was the root of the clone operation
     */
    @Column(name = "is_root_individual", nullable = false)
    @Builder.Default
    private boolean rootIndividual = false;

    /**
     * When the mapping was created
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
