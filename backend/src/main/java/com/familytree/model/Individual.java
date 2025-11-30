package com.familytree.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"tree", "mediaFiles", "events"})
@EqualsAndHashCode(exclude = {"tree", "mediaFiles", "events"})
public class Individual {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tree_id", nullable = false)
    @JsonIgnoreProperties({"individuals", "permissions", "relationships", "owner"})
    private FamilyTree tree;

    @Size(max = 255)
    @Column(name = "given_name")
    private String givenName;

    @Size(max = 255)
    @Column(name = "middle_name")
    private String middleName;

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

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Size(max = 1000)
    @Column(name = "profile_picture_url")
    private String profilePictureUrl;

    @Size(max = 500)
    @Column(name = "facebook_link")
    private String facebookLink;

    @Size(max = 20)
    @Column(name = "phone_number")
    private String phoneNumber;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Relationships
    @OneToMany(mappedBy = "individual", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties({"individual"})
    private Set<Media> mediaFiles = new HashSet<>();

    @OneToMany(mappedBy = "individual", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnoreProperties({"individual"})
    private Set<Event> events = new HashSet<>();

    // Note: Relationships are handled through the Relationship entity
    // to avoid complex bidirectional mappings
}
