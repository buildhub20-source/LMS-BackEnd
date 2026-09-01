package com.lms.assessment.service;

import com.lms.assessment.dto.request.SaveSubmissionRequest;
import com.lms.assessment.dto.response.AssessmentResultReportResponse;
import com.lms.assessment.dto.response.AssessmentSummaryResponse;
import com.lms.assessment.dto.response.AttemptDetailResponse;
import com.lms.assessment.dto.response.AttemptHistoryResponse;
import com.lms.assessment.dto.response.StartAttemptResponse;
import com.lms.assessment.dto.response.StudentQuestionResponse;
import com.lms.assessment.dto.response.StudentTestCaseResponse;
import com.lms.assessment.dto.response.SubmissionResponse;
import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentAttempt;
import com.lms.assessment.entity.AssessmentQuestion;
import com.lms.assessment.entity.AssessmentStatus;
import com.lms.assessment.entity.AttemptStatus;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.RubricScore;
import com.lms.assessment.entity.Submission;
import com.lms.assessment.entity.TestCase;
import com.lms.assessment.mapper.AssessmentMapper;
import com.lms.assessment.repository.AssessmentAttemptRepository;
import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.AssessmentRepository;
import com.lms.assessment.repository.RubricScoreRepository;
import com.lms.assessment.repository.SubmissionRepository;
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentAssessmentServiceImpl implements StudentAssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final TestCaseRepository testCaseRepository;
    private final SubmissionRepository submissionRepository;
    private final RubricScoreRepository rubricScoreRepository;
    private final UserRepository userRepository;
    private final AssessmentMapper assessmentMapper;
    private final com.lms.common.service.StorageService storageService;

    @Override
    public PageResponse<AssessmentSummaryResponse> listPublished(Pageable pageable) {
        Page<Assessment> page =
                assessmentRepository.findByStatusOrderByCreatedAtDesc(AssessmentStatus.PUBLISHED, pageable);

        return PageResponse.from(page, assessment ->
                assessmentMapper.toSummaryResponse(
                        assessment,
                        assessmentQuestionRepository.countByAssessmentId(assessment.getId())
                )
        );
    }

    @Override
    @Transactional
    public StartAttemptResponse startAttempt(UUID assessmentId, UUID studentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assessment", assessmentId));

        if (!assessment.isPublished()) {
            throw new BusinessRuleException("Assessment is not currently available for student attempts");
        }

        // Validate time window if set
        Instant now = Instant.now();
        if (assessment.getStartTime() != null && now.isBefore(assessment.getStartTime())) {
            throw new BusinessRuleException("Assessment has not opened yet");
        }
        if (assessment.getEndTime() != null && now.isAfter(assessment.getEndTime())) {
            throw new BusinessRuleException("Assessment deadline has passed");
        }

        // Check existing attempts
        List<AssessmentAttempt> existingAttempts =
                attemptRepository.findByAssessmentIdAndStudentIdOrderByStartedAtDesc(assessmentId, studentId);

        // If there's an active IN_PROGRESS attempt, return it (resume)
        Optional<AssessmentAttempt> activeAttempt = existingAttempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                .findFirst();

        AssessmentAttempt attempt;
        if (activeAttempt.isPresent()) {
            attempt = activeAttempt.get();
            if (attempt.isExpiredByTime()) {
                handleAttemptExpiry(attempt);
                throw new BusinessRuleException("Your previous attempt has expired and was auto-submitted");
            }
        } else {
            // Check max attempts limit
            if (existingAttempts.size() >= assessment.getMaxAttempts()) {
                throw new BusinessRuleException(
                        "Maximum attempt limit (" + assessment.getMaxAttempts() + ") reached for this assessment");
            }

            // Create new attempt
            Instant expiresAt = now.plus(assessment.getDurationMinutes(), ChronoUnit.MINUTES);
            attempt = AssessmentAttempt.builder()
                    .assessment(assessment)
                    .studentId(studentId)
                    .startedAt(now)
                    .expiresAt(expiresAt)
                    .status(AttemptStatus.IN_PROGRESS)
                    .build();

            attempt = attemptRepository.save(attempt);
            log.info("Student {} started attempt {} for assessment {}", studentId, attempt.getId(), assessmentId);
        }

        long remaining = Math.max(0, Duration.between(now, attempt.getExpiresAt()).getSeconds());
        List<StudentQuestionResponse> questions = getStudentQuestions(assessment, attempt.getId());

        return new StartAttemptResponse(
                attempt.getId(),
                assessment.getId(),
                assessment.getTitle(),
                assessment.getDurationMinutes(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getExpiresAt(),
                remaining,
                questions
        );
    }

    @Override
    @Transactional
    public SubmissionResponse saveSubmission(UUID attemptId, UUID studentId, SaveSubmissionRequest request) {
        AssessmentAttempt attempt = requireAttempt(attemptId, studentId);
        validateAttemptActive(attempt);

        // Upsert submission draft
        Submission submission = submissionRepository.findByAttemptIdAndQuestionId(attemptId, request.questionId())
                .orElseGet(() -> Submission.builder()
                        .attempt(attempt)
                        .questionId(request.questionId())
                        .studentId(studentId)
                        .build());

        submission.setLanguage(request.language());
        submission.setSourceCode(request.sourceCode());
        submission.setStatus("DRAFT");
        submission.setSubmittedAt(Instant.now());

        Submission saved = submissionRepository.save(submission);
        log.debug("Autosaved submission {} for attempt {} question {}", saved.getId(), attemptId, request.questionId());

        return toSubmissionResponse(saved);
    }

    @Override
    @Transactional
    public AttemptDetailResponse submitAttempt(UUID attemptId, UUID studentId) {
        AssessmentAttempt attempt = requireAttempt(attemptId, studentId);

        // If already submitted, return the details idempotently without throwing error
        if (attempt.getStatus() == AttemptStatus.SUBMITTED) {
            List<Submission> allSubmissions = submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId);
            return buildAttemptDetail(attempt, allSubmissions);
        }

        // If expired, auto-finalize draft submissions and return
        if (attempt.getStatus() == AttemptStatus.EXPIRED || attempt.isExpiredByTime()) {
            handleAttemptExpiry(attempt);
            List<Submission> allSubmissions = submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId);
            return buildAttemptDetail(attempt, allSubmissions);
        }

        validateAttemptActive(attempt);

        Instant now = Instant.now();
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(now);
        attemptRepository.save(attempt);

        // Fetch existing submissions
        List<Submission> existingSubmissions = submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId);
        Map<UUID, Submission> submissionByQuestion = existingSubmissions.stream()
                .collect(Collectors.toMap(Submission::getQuestionId, s -> s, (a, b) -> a));

        // Update all draft submissions to SUBMITTED
        for (Submission sub : existingSubmissions) {
            if ("DRAFT".equals(sub.getStatus())) {
                sub.setStatus("SUBMITTED");
                sub.setSubmittedAt(now);
                submissionRepository.save(sub);
            }
        }

        // Ensure any questions without a submission record also get a SUBMITTED entry
        List<AssessmentQuestion> aqList = assessmentQuestionRepository.findByAssessmentIdOrderByQuestionOrderAsc(attempt.getAssessment().getId());
        for (AssessmentQuestion aq : aqList) {
            if (!submissionByQuestion.containsKey(aq.getQuestion().getId())) {
                Submission blankSub = Submission.builder()
                        .attempt(attempt)
                        .questionId(aq.getQuestion().getId())
                        .studentId(studentId)
                        .language("JAVA")
                        .sourceCode("")
                        .status("SUBMITTED")
                        .submittedAt(now)
                        .build();
                submissionRepository.save(blankSub);
            }
        }

        List<Submission> allSubmissions = submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId);
        log.info("Student {} successfully submitted attempt {} (status set to SUBMITTED in DB)", studentId, attemptId);
        return buildAttemptDetail(attempt, allSubmissions);
    }

    @Override
    @Transactional
    public AttemptDetailResponse getAttemptDetail(UUID attemptId, UUID studentId) {
        AssessmentAttempt attempt = requireAttempt(attemptId, studentId);

        // Check if timer elapsed
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS && attempt.isExpiredByTime()) {
            handleAttemptExpiry(attempt);
        }

        List<Submission> submissions = submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId);
        return buildAttemptDetail(attempt, submissions);
    }

    @Override
    public List<AttemptHistoryResponse> getStudentAttemptHistory(UUID assessmentId, UUID studentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assessment", assessmentId));

        List<AssessmentAttempt> attempts =
                attemptRepository.findByAssessmentIdAndStudentIdOrderByStartedAtDesc(assessmentId, studentId);

        int totalAttempts = attempts.size();
        List<AttemptHistoryResponse> history = new ArrayList<>();

        for (int i = 0; i < attempts.size(); i++) {
            AssessmentAttempt a = attempts.get(i);
            int attemptNum = totalAttempts - i;
            int score = a.getScore() != null ? a.getScore() : 0;
            double pct = assessment.getTotalMarks() > 0 ? (score * 100.0 / assessment.getTotalMarks()) : 0.0;

            history.add(new AttemptHistoryResponse(
                    a.getId(),
                    assessment.getId(),
                    assessment.getTitle(),
                    attemptNum,
                    a.getStatus(),
                    a.getScore(),
                    assessment.getTotalMarks(),
                    Math.round(pct * 10.0) / 10.0,
                    a.getStartedAt(),
                    a.getSubmittedAt(),
                    a.getExpiresAt()
            ));
        }

        return history;
    }

    @Override
    public AssessmentResultReportResponse getStudentResultReport(UUID attemptId, UUID studentId) {
        AssessmentAttempt attempt = requireAttempt(attemptId, studentId);
        Assessment assessment = attempt.getAssessment();

        User student = userRepository.findById(studentId).orElse(null);
        String studentName = student != null ? student.getName() : "Student";

        List<AssessmentAttempt> allAttempts =
                attemptRepository.findByAssessmentIdAndStudentIdOrderByStartedAtDesc(assessment.getId(), studentId);

        int finalScore = attempt.getScore() != null ? attempt.getScore() : 0;
        double pct = assessment.getTotalMarks() > 0 ? (finalScore * 100.0 / assessment.getTotalMarks()) : 0.0;
        boolean passed = pct >= 50.0; // 50% threshold default

        Instant endInstant = attempt.getSubmittedAt() != null ? attempt.getSubmittedAt() : Instant.now();
        long timeSpentSeconds = Math.max(0, Duration.between(attempt.getStartedAt(), endInstant).getSeconds());

        // Question results
        List<Submission> submissions = submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId);
        Map<UUID, Submission> submissionMap = submissions.stream()
                .collect(Collectors.toMap(Submission::getQuestionId, s -> s, (s1, s2) -> s1));

        List<AssessmentQuestion> aqs = assessmentQuestionRepository.findByAssessmentIdOrderByQuestionOrderAsc(assessment.getId());
        List<AssessmentResultReportResponse.QuestionResultDto> qResults = new ArrayList<>();

        for (AssessmentQuestion aq : aqs) {
            Question q = aq.getQuestion();
            Submission sub = submissionMap.get(q.getId());

            String subStatus = sub != null ? sub.getStatus() : "NOT_ATTEMPTED";
            String code = sub != null ? sub.getSourceCode() : null;

            List<AssessmentResultReportResponse.RubricEvaluationDto> rEvals = new ArrayList<>();
            if (sub != null) {
                List<RubricScore> rScores = rubricScoreRepository.findBySubmissionId(sub.getId());
                for (RubricScore rs : rScores) {
                    rEvals.add(new AssessmentResultReportResponse.RubricEvaluationDto(
                            rs.getCriterion().getId(),
                            rs.getCriterion().getCriterionName(),
                            rs.getScore(),
                            rs.getCriterion().getMaxPoints(),
                            rs.getFeedback()
                    ));
                }
            }

            int earned = (sub != null && "ACCEPTED".equalsIgnoreCase(sub.getStatus())) ? aq.getMarks() : 0;

            qResults.add(new AssessmentResultReportResponse.QuestionResultDto(
                    q.getId(),
                    q.getTitle(),
                    q.getQuestionType().name(),
                    aq.getMarks(),
                    earned,
                    subStatus,
                    code,
                    rEvals
            ));
        }

        List<AttemptHistoryResponse> history = getStudentAttemptHistory(assessment.getId(), studentId);
        String playbackUrl = getRecordingPlaybackUrl(attempt.getId(), studentId);

        return new AssessmentResultReportResponse(
                attempt.getId(),
                assessment.getId(),
                assessment.getTitle(),
                studentId,
                studentName,
                attempt.getStatus(),
                attempt.getScore(),
                assessment.getTotalMarks(),
                Math.round(pct * 10.0) / 10.0,
                passed,
                assessment.getRetakePolicy() != null ? assessment.getRetakePolicy().name() : "BEST_SCORE",
                allAttempts.size(),
                assessment.getMaxAttempts(),
                timeSpentSeconds,
                attempt.getStartedAt(),
                attempt.getSubmittedAt(),
                playbackUrl,
                attempt.getRecordingDurationSeconds(),
                qResults,
                history
        );
    }

    @Override
    public com.lms.assessment.dto.response.GenerateAttemptRecordingUploadUrlResponse generateRecordingUploadUrl(
            UUID attemptId, UUID studentId, com.lms.assessment.dto.request.GenerateAttemptRecordingUploadUrlRequest request) {
        AssessmentAttempt attempt = requireAttempt(attemptId, studentId);

        String contentType = request.contentType() != null && !request.contentType().isBlank()
                ? request.contentType()
                : "video/webm";
        String objectKey = String.format("assessments/attempts/%s/%s.webm", attemptId, UUID.randomUUID());

        String presignedUrl = storageService.generatePresignedUploadUrl(objectKey, contentType);
        String publicUrl = storageService.getPublicUrl(objectKey);

        log.info("Generated Cloudflare R2 upload URL for attempt {} key {}", attemptId, objectKey);

        return new com.lms.assessment.dto.response.GenerateAttemptRecordingUploadUrlResponse(
                attemptId,
                presignedUrl,
                objectKey,
                publicUrl,
                presignedUrl != null
        );
    }

    @Override
    @Transactional
    public void completeRecordingUpload(
            UUID attemptId, UUID studentId, com.lms.assessment.dto.request.CompleteAttemptRecordingUploadRequest request) {
        AssessmentAttempt attempt = requireAttempt(attemptId, studentId);

        attempt.setRecordingKey(request.key());
        attempt.setRecordingUrl(storageService.getPublicUrl(request.key()));
        if (request.durationSeconds() != null) {
            attempt.setRecordingDurationSeconds(request.durationSeconds());
        }

        attemptRepository.save(attempt);
        log.info("Completed Cloudflare R2 recording upload for attempt {}: key={}", attemptId, request.key());
    }

    @Override
    @Transactional
    public void uploadRecordingDirect(
            UUID attemptId, UUID studentId, org.springframework.web.multipart.MultipartFile file, Integer durationSeconds) {
        AssessmentAttempt attempt = requireAttempt(attemptId, studentId);

        String contentType = file.getContentType() != null ? file.getContentType() : "video/webm";
        String objectKey = String.format("assessments/attempts/%s/%s.webm", attemptId, UUID.randomUUID());

        try {
            storageService.uploadFile(objectKey, file.getInputStream(), file.getSize(), contentType);
            attempt.setRecordingKey(objectKey);
            attempt.setRecordingUrl(storageService.getPublicUrl(objectKey));
            if (durationSeconds != null) {
                attempt.setRecordingDurationSeconds(durationSeconds);
            }
            attemptRepository.save(attempt);
            log.info("Directly uploaded screen recording to Cloudflare R2 for attempt {}", attemptId);
        } catch (Exception e) {
            log.error("Failed to upload screen recording directly for attempt {}: {}", attemptId, e.getMessage(), e);
            throw new com.lms.common.exception.ApplicationException(
                    com.lms.common.exception.ErrorCode.INTERNAL_ERROR,
                    "Failed to upload screen recording to Cloudflare R2: " + e.getMessage());
        }
    }

    @Override
    public String getRecordingPlaybackUrl(UUID attemptId, UUID studentId) {
        AssessmentAttempt attempt = requireAttempt(attemptId, studentId);
        if (attempt.getRecordingKey() == null || attempt.getRecordingKey().isBlank()) {
            return attempt.getRecordingUrl();
        }

        String presignedUrl = storageService.generatePresignedGetUrl(attempt.getRecordingKey());
        return presignedUrl != null ? presignedUrl : attempt.getRecordingUrl();
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void handleAttemptExpiry(AssessmentAttempt attempt) {
        attempt.setStatus(AttemptStatus.EXPIRED);
        Instant now = Instant.now();
        attempt.setSubmittedAt(now);
        attemptRepository.save(attempt);

        List<Submission> submissions = submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attempt.getId());
        for (Submission sub : submissions) {
            if ("DRAFT".equals(sub.getStatus())) {
                sub.setStatus("SUBMITTED");
                sub.setSubmittedAt(now);
                submissionRepository.save(sub);
            }
        }
        log.info("Attempt {} expired by time and auto-submitted draft submissions", attempt.getId());
    }

    private AssessmentAttempt requireAttempt(UUID attemptId, UUID studentId) {
        return attemptRepository.findByIdAndStudentId(attemptId, studentId)
                .orElseThrow(() -> ResourceNotFoundException.of("AssessmentAttempt", attemptId));
    }

    private void validateAttemptActive(AssessmentAttempt attempt) {
        if (attempt.isTerminal()) {
            throw new BusinessRuleException("Assessment attempt is already " + attempt.getStatus());
        }
        if (attempt.isExpiredByTime()) {
            handleAttemptExpiry(attempt);
            throw new BusinessRuleException("Assessment attempt has expired");
        }
    }

    private List<StudentQuestionResponse> getStudentQuestions(Assessment assessment, UUID attemptId) {
        List<AssessmentQuestion> junctions =
                assessmentQuestionRepository.findByAssessmentIdOrderByQuestionOrderAsc(assessment.getId());

        List<StudentQuestionResponse> list = new ArrayList<>();
        for (AssessmentQuestion aq : junctions) {
            Question q = aq.getQuestion();
            List<TestCase> sampleTcs = testCaseRepository.findByQuestionIdAndSampleTrueOrderByIdAsc(q.getId());

            List<StudentTestCaseResponse> tcResponses = sampleTcs.stream()
                    .map(tc -> new StudentTestCaseResponse(tc.getId(), tc.getInputData(), tc.getExpectedOutput()))
                    .toList();

            list.add(new StudentQuestionResponse(
                    q.getId(),
                    q.getTitle(),
                    q.getDescription(),
                    q.getInputFormat(),
                    q.getOutputFormat(),
                    q.getConstraints(),
                    q.getDifficulty(),
                    aq.getMarks(),
                    q.getTimeLimitMs(),
                    q.getMemoryLimitMb(),
                    aq.getQuestionOrder(),
                    tcResponses
            ));
        }

        if (assessment.isRandomizeQuestions() && attemptId != null) {
            // Seed Random with attemptId hashCode for deterministic order per attempt
            Collections.shuffle(list, new Random(attemptId.hashCode()));
        }

        return list;
    }

    private AttemptDetailResponse buildAttemptDetail(AssessmentAttempt attempt, List<Submission> submissions) {
        Assessment assessment = attempt.getAssessment();
        Instant now = Instant.now();
        long remaining = Math.max(0, Duration.between(now, attempt.getExpiresAt()).getSeconds());

        List<StudentQuestionResponse> questions = getStudentQuestions(assessment, attempt.getId());
        List<SubmissionResponse> subResponses = submissions.stream()
                .map(this::toSubmissionResponse)
                .toList();

        return new AttemptDetailResponse(
                attempt.getId(),
                assessment.getId(),
                assessment.getTitle(),
                assessment.getDurationMinutes(),
                attempt.getStatus(),
                attempt.getScore(),
                attempt.getStartedAt(),
                attempt.getExpiresAt(),
                attempt.getSubmittedAt(),
                remaining,
                questions,
                subResponses
        );
    }

    private SubmissionResponse toSubmissionResponse(Submission s) {
        return new SubmissionResponse(
                s.getId(),
                s.getAttempt().getId(),
                s.getQuestionId(),
                s.getLanguage(),
                s.getSourceCode(),
                s.getStatus(),
                s.getSubmittedAt()
        );
    }
}
