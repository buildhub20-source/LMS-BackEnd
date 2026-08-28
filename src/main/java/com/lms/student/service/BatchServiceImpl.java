package com.lms.student.service;

import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceAlreadyExistsException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.PageResponse;
import com.lms.course.repository.CourseRepository;
import com.lms.student.dto.request.CreateBatchRequest;
import com.lms.student.dto.request.UpdateBatchRequest;
import com.lms.student.dto.response.BatchResponse;
import com.lms.student.entity.Batch;
import com.lms.student.entity.BatchStatus;
import com.lms.student.entity.DeliveryMode;
import com.lms.student.mapper.BatchMapper;
import com.lms.student.repository.BatchRepository;
import com.lms.student.repository.StudentBatchRepository;
import com.lms.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BatchServiceImpl implements BatchService {

    private final BatchRepository batchRepository;
    private final StudentBatchRepository enrolmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final BatchMapper batchMapper;

    @Override
    public BatchResponse create(CreateBatchRequest request) {
        String code = request.getCode().trim();
        if (batchRepository.existsByCodeIgnoreCase(code)) {
            throw ResourceAlreadyExistsException.of("Batch code", code);
        }

        requireCourseExists(request.getCourseId());
        requireEndNotBeforeStart(request.getStartDate(), request.getEndDate());

        Batch batch = Batch.builder()
                .code(code)
                .name(request.getName().trim())
                .courseId(request.getCourseId())
                .instructorId(request.getInstructorId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .schedule(trimToNull(request.getSchedule()))
                .deliveryMode(request.getDeliveryMode() == null
                        ? DeliveryMode.OFFLINE
                        : request.getDeliveryMode())
                .capacity(request.getCapacity())
                .status(request.getStatus() == null ? BatchStatus.PLANNED : request.getStatus())
                .build();

        Batch saved = batchRepository.save(batch);
        log.info("Batch {} created starting {}", saved.getCode(), saved.getStartDate());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BatchResponse findById(UUID id) {
        return toResponse(requireBatch(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BatchResponse> search(String search, BatchStatus status, Pageable pageable) {
        Page<Batch> page = batchRepository.findAll(buildSpec(search, status), pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Override
    public BatchResponse update(UUID id, UpdateBatchRequest request) {
        Batch batch = requireBatch(id);

        if (StringUtils.hasText(request.getName()))     batch.setName(request.getName().trim());
        if (request.getCourseId() != null) {
            requireCourseExists(request.getCourseId());
            batch.setCourseId(request.getCourseId());
        }
        if (request.getInstructorId() != null)          batch.setInstructorId(request.getInstructorId());
        if (request.getStartDate() != null)             batch.setStartDate(request.getStartDate());
        if (request.getEndDate() != null)               batch.setEndDate(request.getEndDate());
        if (request.getSchedule() != null)              batch.setSchedule(trimToNull(request.getSchedule()));
        if (request.getDeliveryMode() != null)          batch.setDeliveryMode(request.getDeliveryMode());
        if (request.getStatus() != null)                batch.setStatus(request.getStatus());

        if (request.getCapacity() != null) {
            long enrolled = enrolmentRepository.countByBatchId(batch.getId());
            if (request.getCapacity() < enrolled) {
                throw new BusinessRuleException("Capacity cannot be set below the "
                        + enrolled + " learners already enrolled");
            }
            batch.setCapacity(request.getCapacity());
        }

        // Checked after the assignments: start and end can move independently
        // and only the resulting pair has to be consistent.
        requireEndNotBeforeStart(batch.getStartDate(), batch.getEndDate());

        return toResponse(batchRepository.save(batch));
    }

    @Override
    public void delete(UUID id) {
        Batch batch = requireBatch(id);

        // The foreign key is RESTRICT, so this would fail at the database anyway;
        // catching it here produces a message staff can act on.
        if (enrolmentRepository.existsByBatchId(id)) {
            throw new BusinessRuleException(
                    "Batch " + batch.getCode() + " still has learners enrolled. "
                            + "Move them out, or cancel the batch instead of deleting it.");
        }

        batchRepository.delete(batch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchResponse> findByInstructor(UUID instructorUserId) {
        return toResponses(batchRepository.findByInstructorIdOrderByStartDateDesc(instructorUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchResponse> findOpenForEnrolment() {
        return toResponses(batchRepository.findByStatusInOrderByStartDateDesc(
                List.of(BatchStatus.PLANNED, BatchStatus.ONGOING)));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    /**
     * Maps a list of batches, resolving course titles, instructor names and
     * enrolment counts in three queries rather than three per row.
     */
    private List<BatchResponse> toResponses(List<Batch> batches) {
        if (batches.isEmpty()) {
            return List.of();
        }

        Set<UUID> courseIds = batches.stream()
                .map(Batch::getCourseId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<UUID> userIds = batches.stream()
                .map(Batch::getInstructorId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());

        Map<UUID, String> courseTitles = courseIds.isEmpty() ? Map.of()
                : courseRepository.findAllById(courseIds).stream()
                        .collect(Collectors.toMap(course -> course.getId(), course -> course.getTitle()));

        Map<UUID, String> userNames = userIds.isEmpty() ? Map.of()
                : userRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(user -> user.getId(), user -> user.getName()));

        Map<UUID, Long> counts = new HashMap<>();
        batches.forEach(batch -> counts.put(batch.getId(),
                enrolmentRepository.countByBatchId(batch.getId())));

        List<BatchResponse> responses = new ArrayList<>(batches.size());
        batches.forEach(batch -> responses.add(
                batchMapper.toResponse(batch, courseTitles, userNames, counts.get(batch.getId()))));
        return responses;
    }

    private BatchResponse toResponse(Batch batch) {
        return toResponses(List.of(batch)).get(0);
    }

    private Specification<Batch> buildSpec(String search, BatchStatus status) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), pattern),
                        cb.like(cb.lower(root.get("name")), pattern)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Batch requireBatch(UUID id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Batch", id));
    }

    private void requireCourseExists(UUID courseId) {
        if (courseId != null && !courseRepository.existsById(courseId)) {
            throw ResourceNotFoundException.of("Course", courseId);
        }
    }

    private void requireEndNotBeforeStart(java.time.LocalDate start, java.time.LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new BusinessRuleException("The batch end date cannot be before its start date");
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
