package com.lms.course.entity;

import com.lms.common.audit.Timestamped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Metadata for a recording stored in an external object store (e.g. Cloudflare R2).
 *
 * <p>The database never stores the binary — only the storage key and file metadata.
 * Currently at the course level (intro/preview video). Will be extended to
 * lesson-level when the lesson domain is built.
 */
@Entity
@Table(name = "course_recordings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRecording extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Builder.Default
    @Column(name = "storage_provider", nullable = false, length = 30)
    private String storageProvider = "CLOUDFLARE_R2";

    /** The object key in R2 — e.g. courses/{courseId}/recordings/{id}.mp4 */
    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RecordingStatus status = RecordingStatus.PENDING;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof CourseRecording rec)) return false;
        return id != null && id.equals(rec.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
