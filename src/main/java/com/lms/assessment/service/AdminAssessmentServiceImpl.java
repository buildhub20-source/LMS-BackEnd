package com.lms.assessment.service;

import com.lms.assessment.dto.request.CreateAssessmentRequest;
import com.lms.assessment.dto.request.UpdateAssessmentRequest;
import com.lms.assessment.dto.response.AssessmentAnalyticsResponse;
import com.lms.assessment.dto.response.AssessmentResponse;
import com.lms.assessment.dto.response.AssessmentSummaryResponse;
import com.lms.assessment.dto.response.GradeDistributionDto;
import com.lms.assessment.dto.response.ScoreDistributionBucketDto;
import com.lms.assessment.dto.response.StudentAssessmentStatDto;
import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentAttempt;
import com.lms.assessment.entity.AssessmentStatus;
import com.lms.assessment.entity.AttemptStatus;
import com.lms.assessment.entity.RetakePolicy;
import com.lms.assessment.mapper.AssessmentMapper;
import com.lms.assessment.repository.AssessmentAttemptRepository;
import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.AssessmentRepository;
import com.lms.assessment.repository.TestCaseRepository;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.PageResponse;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final AssessmentAttemptRepository assessmentAttemptRepository;
    private final UserRepository userRepository;

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public AssessmentResponse create(CreateAssessmentRequest request, UUID createdBy) {
        CreateAssessmentRequest req = request.withDefaults();

        RetakePolicy policy = RetakePolicy.BEST_SCORE;
        if (req.retakePolicy() != null) {
            try {
                policy = RetakePolicy.valueOf(req.retakePolicy().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Assessment assessment = Assessment.builder()
                .title(req.title().trim())
                .description(req.description())
                .durationMinutes(req.durationMinutes())
                .totalMarks(req.totalMarks())
                .maxAttempts(req.maxAttempts())
                .randomizeQuestions(Boolean.TRUE.equals(req.randomizeQuestions()))
                .retakePolicy(policy)
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
        if (request.randomizeQuestions() != null) {
            assessment.setRandomizeQuestions(request.randomizeQuestions());
        }
        if (StringUtils.hasText(request.retakePolicy())) {
            try {
                assessment.setRetakePolicy(RetakePolicy.valueOf(request.retakePolicy().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
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

    @Override
    public AssessmentAnalyticsResponse getAnalytics(UUID id) {
        Assessment assessment = requireAssessment(id);

        List<AssessmentAttempt> attempts = assessmentAttemptRepository
                .findByAssessmentIdOrderByStartedAtDesc(id);

        Map<UUID, List<AssessmentAttempt>> attemptsByStudent = attempts.stream()
                .collect(Collectors.groupingBy(AssessmentAttempt::getStudentId));

        Set<UUID> studentIds = new HashSet<>(attemptsByStudent.keySet());

        long totalEnrolled = studentIds.size();
        long attendedCount = studentIds.size();
        long nonAttendedCount = 0;
        long completedCount = attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED || a.getStatus() == AttemptStatus.EXPIRED)
                .count();
        long inProgressCount = attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                .count();

        List<AssessmentAttempt> completedAttempts = attempts.stream()
                .filter(a -> a.getScore() != null)
                .toList();

        double averageScore = completedAttempts.isEmpty() ? 0.0 :
                completedAttempts.stream().mapToInt(AssessmentAttempt::getScore).average().orElse(0.0);

        int totalMarks = assessment.getTotalMarks();
        double averageCompletionPercentage = (totalMarks > 0) ? (averageScore / totalMarks) * 100.0 : 0.0;

        int highestScore = completedAttempts.stream().mapToInt(AssessmentAttempt::getScore).max().orElse(0);
        int lowestScore = completedAttempts.stream().mapToInt(AssessmentAttempt::getScore).min().orElse(0);

        long pendingGradingCount = attempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED && a.getScore() == null)
                .count();

        long passedCount = 0;
        long failedCount = 0;
        long gradeA = 0, gradeB = 0, gradeC = 0, gradeD = 0, gradeF = 0;

        List<StudentAssessmentStatDto> studentStats = new ArrayList<>();
        for (UUID studentId : studentIds) {
            List<AssessmentAttempt> userAttempts = attemptsByStudent.get(studentId);
            AssessmentAttempt latestAttempt = userAttempts.get(0);

            Optional<User> userOpt = userRepository.findById(studentId);
            String studentName = userOpt.map(User::getName).orElse("Student");
            String studentEmail = userOpt.map(User::getEmail).orElse("");

            Optional<AssessmentAttempt> completedOpt = userAttempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED)
                    .findFirst();

            String status = completedOpt.isPresent() ? "SUBMITTED" : latestAttempt.getStatus().name();
            Integer score = completedOpt.map(AssessmentAttempt::getScore)
                    .orElse(latestAttempt.getScore());
            Double pct = (score != null && totalMarks > 0) ? (score.doubleValue() / totalMarks) * 100.0 : 0.0;

            String gradeLetter = "F";
            boolean passed = pct >= 50.0;
            if (pct >= 90.0) gradeLetter = "A";
            else if (pct >= 80.0) gradeLetter = "B";
            else if (pct >= 70.0) gradeLetter = "C";
            else if (pct >= 60.0) gradeLetter = "D";

            if (score != null) {
                if (passed) passedCount++; else failedCount++;
                switch (gradeLetter) {
                    case "A" -> gradeA++;
                    case "B" -> gradeB++;
                    case "C" -> gradeC++;
                    case "D" -> gradeD++;
                    default -> gradeF++;
                }
            }

            studentStats.add(new StudentAssessmentStatDto(
                    studentId,
                    studentName,
                    studentEmail,
                    status,
                    score,
                    totalMarks,
                    Math.round(pct * 10.0) / 10.0,
                    gradeLetter,
                    passed,
                    userAttempts.size(),
                    latestAttempt.getSubmittedAt() != null ? latestAttempt.getSubmittedAt() : latestAttempt.getStartedAt()
            ));
        }

        double passPercentage = completedCount > 0 ? (passedCount * 100.0 / completedCount) : 0.0;

        long b1 = 0, b2 = 0, b3 = 0, b4 = 0;
        for (AssessmentAttempt a : completedAttempts) {
            if (a.getScore() != null && totalMarks > 0) {
                double pct = (a.getScore().doubleValue() / totalMarks) * 100.0;
                if (pct <= 25.0) b1++;
                else if (pct <= 50.0) b2++;
                else if (pct <= 75.0) b3++;
                else b4++;
            }
        }

        List<ScoreDistributionBucketDto> scoreDistribution = List.of(
                new ScoreDistributionBucketDto("0-25%", b1),
                new ScoreDistributionBucketDto("26-50%", b2),
                new ScoreDistributionBucketDto("51-75%", b3),
                new ScoreDistributionBucketDto("76-100%", b4)
        );

        long totalGraded = completedAttempts.size();
        List<GradeDistributionDto> gradeDistribution = List.of(
                new GradeDistributionDto("A", "90-100%", gradeA, totalGraded > 0 ? Math.round((gradeA * 100.0 / totalGraded) * 10.0) / 10.0 : 0.0),
                new GradeDistributionDto("B", "80-89%", gradeB, totalGraded > 0 ? Math.round((gradeB * 100.0 / totalGraded) * 10.0) / 10.0 : 0.0),
                new GradeDistributionDto("C", "70-79%", gradeC, totalGraded > 0 ? Math.round((gradeC * 100.0 / totalGraded) * 10.0) / 10.0 : 0.0),
                new GradeDistributionDto("D", "60-69%", gradeD, totalGraded > 0 ? Math.round((gradeD * 100.0 / totalGraded) * 10.0) / 10.0 : 0.0),
                new GradeDistributionDto("F", "<60%", gradeF, totalGraded > 0 ? Math.round((gradeF * 100.0 / totalGraded) * 10.0) / 10.0 : 0.0)
        );

        return new AssessmentAnalyticsResponse(
                assessment.getId(),
                assessment.getTitle(),
                assessment.getTotalMarks(),
                totalEnrolled,
                attendedCount,
                nonAttendedCount,
                completedCount,
                inProgressCount,
                pendingGradingCount,
                passedCount,
                failedCount,
                Math.round(passPercentage * 10.0) / 10.0,
                Math.round(averageScore * 10.0) / 10.0,
                Math.round(averageCompletionPercentage * 10.0) / 10.0,
                highestScore,
                lowestScore,
                studentStats,
                scoreDistribution,
                gradeDistribution
        );
    }
}
