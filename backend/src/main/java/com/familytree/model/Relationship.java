package com.familytree.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Relationship entity representing connections between individuals
 */
@Entity
@Table(name = "relationships", indexes = {
    @Index(name = "idx_relationship_tree", columnList = "tree_id"),
    @Index(name = "idx_relationship_ind1", columnList = "individual1_id"),
    @Index(name = "idx_relationship_ind2", columnList = "individual2_id"),
    @Index(name = "idx_relationship_type", columnList = "type")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Relationship {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_id", nullable = false)
    private FamilyTree tree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "individual1_id", nullable = false)
    private Individual individual1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "individual2_id", nullable = false)
    private Individual individual2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RelationshipType type;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
