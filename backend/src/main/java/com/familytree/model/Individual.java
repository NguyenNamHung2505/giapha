package com.familytree.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Individual entity representing a person in the family tree
 */
@Entity
@Table(name = "individuals", indexes = {
    @Index(name = "idx_individual_tree", columnList = "tree_id"),
    @Index(name = "idx_individual_name", columnList = "given_name, surname"),
    @Index(name = "idx_individual_birth", columnList = "birth_date"),
    @Index(name = "idx_individual_death", columnList = "death_date")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Individual {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_id", nullable = false)
    private FamilyTree tree;

    @Size(max = 255)
    @Column(name = "given_name")
    private String givenName;

    @Size(max = 255)
    @Column(name = "surname")
    private String surname;

    @Size(max = 50)
    @Column(name = "suffix")
    private String suffix;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Size(max = 500)
    @Column(name = "birth_place")
    private String birthPlace;

    @Column(name = "death_date")
    private LocalDate deathDate;

    @Size(max = 500)
    @Column(name = "death_place")
    private String deathPlace;

    @Column(columnDefinition = "TEXT")
    private String biography;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "individual", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Media> mediaFiles = new HashSet<>();

    @OneToMany(mappedBy = "individual", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Event> events = new HashSet<>();

    // Note: Relationships are handled through the Relationship entity
    // to avoid complex bidirectional mappings
}
