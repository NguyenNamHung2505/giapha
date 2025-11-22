package com.familytree.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Media entity representing photos and documents attached to individuals
 */
@Entity
@Table(name = "media", indexes = {
    @Index(name = "idx_media_individual", columnList = "individual_id"),
    @Index(name = "idx_media_type", columnList = "type"),
    @Index(name = "idx_media_uploaded", columnList = "uploaded_at")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "individual_id", nullable = false)
    private Individual individual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType type;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false)
    private String filename;

    @NotBlank
    @Size(max = 1000)
    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    @Column(columnDefinition = "TEXT")
    private String caption;

    @Column(name = "file_size")
    private Long fileSize;

    @Size(max = 100)
    @Column(name = "mime_type")
    private String mimeType;

    @CreatedDate
    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
