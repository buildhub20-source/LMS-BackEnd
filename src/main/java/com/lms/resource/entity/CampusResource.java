package com.lms.resource.entity;

import com.lms.common.audit.Timestamped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.UUID;

/**
 * Metadata for a global campus resource or study toolkit file stored in Cloudflare R2.
 */
@Entity
@Table(name = "campus_resources")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampusResource extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    /** Cloudflare R2 object key — e.g. resources/{resourceId}/{filename} */
    @Column(name = "file_key", nullable = false, length = 1024)
    private String fileKey;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "file_size", length = 50)
    private String fileSize;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Builder.Default
    @Column(name = "target_audience", nullable = false, length = 50)
    private String targetAudience = "ALL_STUDENTS";

    @Column(name = "author_id")
    private UUID authorId;

    @Column(name = "author_name")
    private String authorName;

    @Column(name = "author_role", length = 50)
    private String authorRole;

    @Builder.Default
    @Column(name = "downloads_count", nullable = false)
    private Integer downloadsCount = 0;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "PUBLISHED";

    @Builder.Default
    @Column(name = "storage_provider", nullable = false, length = 30)
    private String storageProvider = "CLOUDFLARE_R2";

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CampusResource res)) return false;
        return id != null && id.equals(res.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
