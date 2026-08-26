package com.lms.assessment.service;

import com.lms.assessment.dto.request.SaveSubmissionRequest;
import com.lms.assessment.dto.response.AssessmentSummaryResponse;
import com.lms.assessment.dto.response.AttemptDetailResponse;
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
import com.lms.assessment.entity.Submission;
import com.lms.assessment.entity.TestCase;
import com.lms.assessment.mapper.AssessmentMapper;
import com.lms.assessment.repository.AssessmentAttemptRepository;
import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.AssessmentRepository;
import com.lms.assessment.repository.SubmissionRepository;
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private final AssessmentMapper assessmentMapper;

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
                attempt.setStatus(AttemptStatus.EXPIRED);
                attemptRepository.save(attempt);
                throw new BusinessRuleException("Your previous attempt has expired");
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
        List<StudentQuestionResponse> questions = getStudentQuestions(assessmentId);

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
        validateAttemptActive(attempt);

        Instant now = Instant.now();
        attempt.setStatus(AttemptStatus.SUBMITTED);
        attempt.setSubmittedAt(now);
        attemptRepository.save(attempt);

        // Update all draft submissions to SUBMITTED
        List<Submission> submissions = submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId);
        for (Submission sub : submissions) {
            if ("DRAFT".equals(sub.getStatus())) {
                sub.setStatus("SUBMITTED");
                sub.setSubmittedAt(now);
                submissionRepository.save(sub);
            }
        }

        log.info("Student {} submitted attempt {}", studentId, attemptId);
        return buildAttemptDetail(attempt, submissions);
    }

    @Override
    public AttemptDetailResponse getAttemptDetail(UUID attemptId, UUID studentId) {
        AssessmentAttempt attempt = requireAttempt(attemptId, studentId);

        // Check if timer elapsed
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS && attempt.isExpiredByTime()) {
            attempt.setStatus(AttemptStatus.EXPIRED);
            attemptRepository.save(attempt);
        }

        List<Submission> submissions = submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId);
        return buildAttemptDetail(attempt, submissions);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private AssessmentAttempt requireAttempt(UUID attemptId, UUID studentId) {
        return attemptRepository.findByIdAndStudentId(attemptId, studentId)
                .orElseThrow(() -> ResourceNotFoundException.of("AssessmentAttempt", attemptId));
    }

    private void validateAttemptActive(AssessmentAttempt attempt) {
        if (attempt.isTerminal()) {
            throw new BusinessRuleException("Assessment attempt is already " + attempt.getStatus());
        }
        if (attempt.isExpiredByTime()) {
            attempt.setStatus(AttemptStatus.EXPIRED);
            attemptRepository.save(attempt);
            throw new BusinessRuleException("Assessment attempt has expired");
        }
    }

    private List<StudentQuestionResponse> getStudentQuestions(UUID assessmentId) {
        List<AssessmentQuestion> junctions =
                assessmentQuestionRepository.findByAssessmentIdOrderByQuestionOrderAsc(assessmentId);

        List<StudentQuestionResponse> list = new ArrayList<>();
        for (AssessmentQuestion aq : junctions) {
            Question q = aq.getQuestion();
            // Fetch ONLY sample test cases for students
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
        return list;
    }

    private AttemptDetailResponse buildAttemptDetail(AssessmentAttempt attempt, List<Submission> submissions) {
        Assessment assessment = attempt.getAssessment();
        Instant now = Instant.now();
        long remaining = Math.max(0, Duration.between(now, attempt.getExpiresAt()).getSeconds());

        List<StudentQuestionResponse> questions = getStudentQuestions(assessment.getId());
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
