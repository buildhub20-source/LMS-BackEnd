package com.lms.course.entity;

import com.lms.common.audit.Timestamped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OrderBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A learning course managed by the LMS.
 *
 * <p>{@code createdBy} is the user who originally created the course (admin or instructor).
 * {@code instructorId} is the currently assigned instructor and may differ from the creator
 * or be {@code null} for an unassigned course.
 *
 * <p>Lifecycle transitions are enforced in {@link com.lms.course.service.CourseServiceImpl}.
 */
@Entity
@Table(name = "courses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "level", length = 30)
    private String level;

    @Column(name = "thumbnail_key", length = 512)
    private String thumbnailKey;

    /** The user who created the course — never changes after creation. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    /** Currently assigned instructor — may be null for unassigned courses. */
    @Column(name = "instructor_id")
    private UUID instructorId;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /** Populated when an admin rejects a PENDING_REVIEW course. */
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Builder.Default
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<CourseModule> modules = new ArrayList<>();

    public void addModule(CourseModule module) {
        modules.add(module);
        module.setCourse(this);
    }

    public void removeModule(CourseModule module) {
        modules.remove(module);
        module.setCourse(null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof Course course)) return false;
        return id != null && id.equals(course.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
