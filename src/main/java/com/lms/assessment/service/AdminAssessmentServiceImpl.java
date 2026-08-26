package com.lms.assessment.service;

import com.lms.assessment.dto.request.CreateAssessmentRequest;
import com.lms.assessment.dto.request.UpdateAssessmentRequest;
import com.lms.assessment.dto.response.AssessmentResponse;
import com.lms.assessment.dto.response.AssessmentSummaryResponse;
import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentStatus;
import com.lms.assessment.mapper.AssessmentMapper;
import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.AssessmentRepository;
import com.lms.assessment.repository.TestCaseRepository;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAssessmentServiceImpl implements AdminAssessmentService {

    private static final String RESOURCE = "Assessment";

    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final TestCaseRepository testCaseRepository;
    private final AssessmentMapper assessmentMapper;

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public AssessmentResponse create(CreateAssessmentRequest request, UUID createdBy) {
        CreateAssessmentRequest req = request.withDefaults();

        Assessment assessment = Assessment.builder()
                .title(req.title().trim())
                .description(req.description())
                .durationMinutes(req.durationMinutes())
                .totalMarks(req.totalMarks())
                .maxAttempts(req.maxAttempts())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .status(AssessmentStatus.DRAFT)
                .createdBy(createdBy)
                .build();

        Assessment saved = assessmentRepository.save(assessment);
        log.info("Admin {} created assessment {} ('{}')", createdBy, saved.getId(), saved.getTitle());

        return toResponse(saved);
    }

    // ---------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------

    @Override
    public PageResponse<AssessmentSummaryResponse> list(AssessmentStatus status, Pageable pageable) {
        Page<Assessment> page = (status != null)
                ? assessmentRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : assessmentRepository.findAllByOrderByCreatedAtDesc(pageable);

        return PageResponse.from(page, assessment ->
                assessmentMapper.toSummaryResponse(
                        assessment,
                        assessmentQuestionRepository.countByAssessmentId(assessment.getId())
                )
        );
    }

    @Override
    public AssessmentResponse findById(UUID id) {
        Assessment assessment = requireAssessment(id);
        return toResponse(assessment);
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public AssessmentResponse update(UUID id, UpdateAssessmentRequest request) {
        Assessment assessment = requireAssessment(id);
        requireDraft(assessment, "update");

        if (StringUtils.hasText(request.title())) {
            assessment.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            assessment.setDescription(request.description());
        }
        if (request.durationMinutes() != null) {
            assessment.setDurationMinutes(request.durationMinutes());
        }
        // totalMarks is intentionally NOT updated here — it is auto-computed
        // from the sum of all AssessmentQuestion.marks values.
        if (request.maxAttempts() != null) {
            assessment.setMaxAttempts(request.maxAttempts());
        }
        // Null start/end times are meaningful (clearing an existing window)
        if (request.startTime() != null || request.endTime() != null) {
            assessment.setStartTime(request.startTime());
            assessment.setEndTime(request.endTime());
        }

        Assessment saved = assessmentRepository.save(assessment);
        log.debug("Admin updated assessment {}", id);
        return toResponse(saved);
    }

    // ---------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void delete(UUID id) {
        Assessment assessment = requireAssessment(id);
        requireDraft(assessment, "delete");

        assessmentRepository.delete(assessment);
        log.info("Admin deleted draft assessment {}", id);
    }

    // ---------------------------------------------------------------
    // Publish / Unpublish / Close / Archive
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public AssessmentResponse publish(UUID id) {
        Assessment assessment = requireAssessment(id);

        if (!assessment.isDraft()) {
            throw new BusinessRuleException(
                    "Assessment cannot be published: current status is " + assessment.getStatus());
        }

        validateForPublishing(assessment);

        assessment.publish();
        Assessment saved = assessmentRepository.save(assessment);
        log.info("Admin published assessment {} ('{}')", id, assessment.getTitle());

        return toResponse(saved);
    }

    @Override
    @Transactional
    public AssessmentResponse unpublish(UUID id) {
        Assessment assessment = requireAssessment(id);

        if (!assessment.isPublished()) {
            throw new BusinessRuleException(
                    "Assessment cannot be unpublished: current status is " + assessment.getStatus()
                    + ". Only PUBLISHED assessments can be moved back to DRAFT.");
        }

        assessment.setStatus(AssessmentStatus.DRAFT);
        Assessment saved = assessmentRepository.save(assessment);
        log.info("Admin unpublished assessment {} — moved back to DRAFT", id);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public AssessmentResponse close(UUID id) {
        Assessment assessment = requireAssessment(id);

        if (!assessment.isPublished()) {
            throw new BusinessRuleException(
                    "Assessment cannot be closed: current status is " + assessment.getStatus()
                    + ". Only PUBLISHED assessments can be closed.");
        }

        assessment.close();
        Assessment saved = assessmentRepository.save(assessment);
        log.info("Admin closed assessment {} — no new attempts allowed", id);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public AssessmentResponse archive(UUID id) {
        Assessment assessment = requireAssessment(id);

        if (assessment.getStatus() == AssessmentStatus.ARCHIVED) {
            throw new BusinessRuleException("Assessment is already archived.");
        }

        assessment.archive();
        Assessment saved = assessmentRepository.save(assessment);
        log.info("Admin archived assessment {}", id);

        return toResponse(saved);
    }

    // ---------------------------------------------------------------
    // Validation helpers
    // ---------------------------------------------------------------

    private void validateForPublishing(Assessment assessment) {
        // 1. Title
        if (!StringUtils.hasText(assessment.getTitle())) {
            throw new BusinessRuleException("Assessment title is required before publishing");
        }

        // 2. Duration
        if (assessment.getDurationMinutes() <= 0) {
            throw new BusinessRuleException("Assessment duration must be greater than 0");
        }

        // 3. Total marks
        if (assessment.getTotalMarks() <= 0) {
            throw new BusinessRuleException("Assessment total marks must be greater than 0");
        }

        // 4. At least one question
        long questionCount = assessmentQuestionRepository.countByAssessmentId(assessment.getId());
        if (questionCount == 0) {
            throw new BusinessRuleException(
                    "Assessment must have at least one question before publishing");
        }

        // 5. Every question must have at least one test case
        List<UUID> questionsWithTestCases =
                assessmentQuestionRepository.findQuestionIdsWithTestCases(assessment.getId());

        if (questionsWithTestCases.size() < questionCount) {
            throw new BusinessRuleException(
                    "Every question must have at least one test case before publishing");
        }
    }

    // ---------------------------------------------------------------
    // Private utilities
    // ---------------------------------------------------------------

    private Assessment requireAssessment(UUID id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
    }

    private static void requireDraft(Assessment assessment, String action) {
        if (!assessment.isDraft()) {
            throw new BusinessRuleException(
                    "Cannot " + action + " assessment: status is " + assessment.getStatus()
                    + ". Only DRAFT assessments may be " + action + "d.");
        }
    }

    private AssessmentResponse toResponse(Assessment assessment) {
        long count = assessmentQuestionRepository.countByAssessmentId(assessment.getId());
        return assessmentMapper.toResponse(assessment, count);
    }
}
