package com.lms.course.service;

import com.lms.common.audit.AuditAction;
import com.lms.common.audit.AuditService;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.PageResponse;
import com.lms.course.dto.request.CreateCourseRequest;
import com.lms.course.dto.request.RejectCourseRequest;
import com.lms.course.dto.request.UpdateCourseRequest;
import com.lms.course.dto.response.CourseResponse;
import com.lms.course.entity.Course;
import com.lms.course.entity.CourseStatus;
import com.lms.course.mapper.CourseMapper;
import com.lms.course.repository.CourseRepository;
import com.lms.security.authentication.AuthenticationService;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourseServiceImpl implements CourseService {

    private static final String RESOURCE = "COURSE";

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;
    private final AuditService auditService;
    private final UserRepository userRepository;

    // ─── CRUD ────────────────────────────────────────────────────────────────

    @Override
    public CourseResponse create(CreateCourseRequest request) {
        UUID actorId = requireCurrentUserId();

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .level(request.getLevel())
                .durationMinutes(request.getDurationMinutes())
                .createdBy(actorId)
                .instructorId(actorId)
                .status(CourseStatus.DRAFT)
                .build();

        Course saved = courseRepository.save(course);
        auditService.record(AuditAction.COURSE_CREATED, RESOURCE, saved.getId(),
                "Course created: " + saved.getTitle());

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse findById(UUID id) {
        return toResponse(requireCourse(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CourseResponse> search(String search, CourseStatus status, Pageable pageable) {
        Specification<Course> spec = buildSpec(search, status);
        Page<Course> page = courseRepository.findAll(spec, pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Override
    public CourseResponse update(UUID id, UpdateCourseRequest request) {
        Course course = requireCourse(id);

        if (StringUtils.hasText(request.getTitle()))       course.setTitle(request.getTitle());
        if (StringUtils.hasText(request.getDescription())) course.setDescription(request.getDescription());
        if (StringUtils.hasText(request.getLevel()))       course.setLevel(request.getLevel());
        if (request.getDurationMinutes() != null)          course.setDurationMinutes(request.getDurationMinutes());
        if (request.getInstructorId() != null)             course.setInstructorId(request.getInstructorId());

        Course saved = courseRepository.save(course);
        auditService.record(AuditAction.COURSE_UPDATED, RESOURCE, saved.getId(), null);
        return toResponse(saved);
    }

    @Override
    public void delete(UUID id) {
        Course course = requireCourse(id);
        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new BusinessRuleException("Only DRAFT courses can be deleted. Archive published courses instead.");
        }
        auditService.record(AuditAction.COURSE_DELETED, RESOURCE, id,
                "Course deleted: " + course.getTitle());
        courseRepository.delete(course);
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public CourseResponse publish(UUID id) {
        Course course = requireCourse(id);
        requireStatus(course, "publish", CourseStatus.DRAFT, CourseStatus.UNPUBLISHED);
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(Instant.now());
        course.setRejectionReason(null);
        Course saved = courseRepository.save(course);
        auditService.record(AuditAction.COURSE_PUBLISHED, RESOURCE, saved.getId(), null);
        return toResponse(saved);
    }

    @Override
    public CourseResponse unpublish(UUID id) {
        Course course = requireCourse(id);
        requireStatus(course, "unpublish", CourseStatus.PUBLISHED);
        course.setStatus(CourseStatus.UNPUBLISHED);
        Course saved = courseRepository.save(course);
        auditService.record(AuditAction.COURSE_UNPUBLISHED, RESOURCE, saved.getId(), null);
        return toResponse(saved);
    }

    @Override
    public CourseResponse archive(UUID id) {
        Course course = requireCourse(id);
        requireStatus(course, "archive", CourseStatus.PUBLISHED, CourseStatus.UNPUBLISHED);
        course.setStatus(CourseStatus.ARCHIVED);
        course.setArchivedAt(Instant.now());
        Course saved = courseRepository.save(course);
        auditService.record(AuditAction.COURSE_ARCHIVED, RESOURCE, saved.getId(), null);
        return toResponse(saved);
    }

    @Override
    public CourseResponse submit(UUID id) {
        Course course = requireCourse(id);
        requireStatus(course, "submit", CourseStatus.DRAFT);
        course.setStatus(CourseStatus.PENDING_REVIEW);
        course.setRejectionReason(null);
        Course saved = courseRepository.save(course);
        auditService.record(AuditAction.COURSE_SUBMITTED, RESOURCE, saved.getId(), null);
        return toResponse(saved);
    }

    @Override
    public CourseResponse approve(UUID id) {
        Course course = requireCourse(id);
        requireStatus(course, "approve", CourseStatus.PENDING_REVIEW);
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(Instant.now());
        course.setRejectionReason(null);
        Course saved = courseRepository.save(course);
        auditService.record(AuditAction.COURSE_APPROVED, RESOURCE, saved.getId(), null);
        return toResponse(saved);
    }

    @Override
    public CourseResponse reject(UUID id, RejectCourseRequest request) {
        Course course = requireCourse(id);
        requireStatus(course, "reject", CourseStatus.PENDING_REVIEW);
        course.setStatus(CourseStatus.DRAFT);
        course.setRejectionReason(request != null ? request.getReason() : null);
        Course saved = courseRepository.save(course);
        auditService.record(AuditAction.COURSE_REJECTED, RESOURCE, saved.getId(),
                request != null ? request.getReason() : null);
        return toResponse(saved);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private Course requireCourse(UUID id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
    }

    private UUID requireCurrentUserId() {
        return AuthenticationService.requirePrincipal().getUserId();
    }

    /**
     * Guards a lifecycle action: throws if the course is not in one of the allowed statuses.
     */
    private void requireStatus(Course course, String action, CourseStatus... allowed) {
        for (CourseStatus s : allowed) {
            if (course.getStatus() == s) return;
        }
        String allowedList = java.util.Arrays.stream(allowed)
                .map(CourseStatus::name)
                .collect(Collectors.joining(", "));
        throw new BusinessRuleException(
                String.format("Cannot %s a course with status %s. Allowed: [%s]",
                        action, course.getStatus(), allowedList));
    }

    private Specification<Course> buildSpec(String search, CourseStatus status) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (StringUtils.hasText(search)) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /**
     * Resolves creator and instructor names in a single query to avoid N+1,
     * then maps to a response DTO.
     */
    private CourseResponse toResponse(Course course) {
        Map<UUID, String> names = resolveNames(List.of(course));
        return courseMapper.toResponse(course, names);
    }

    private Map<UUID, String> resolveNames(List<Course> courses) {
        var ids = courses.stream()
                .flatMap(c -> {
                    var list = new java.util.ArrayList<UUID>();
                    list.add(c.getCreatedBy());
                    if (c.getInstructorId() != null) list.add(c.getInstructorId());
                    return list.stream();
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        if (ids.isEmpty()) return Map.of();

        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(
                        com.lms.user.entity.User::getId,
                        com.lms.user.entity.User::getName));
    }
}
