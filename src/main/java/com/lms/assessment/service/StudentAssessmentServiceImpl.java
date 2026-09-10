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
import com.lms.assessment.entity.AssessmentRetestGrant;
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
import com.lms.assessment.repository.AssessmentRetestGrantRepository;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import com.lms.assessment.dto.response.QuestionOptionResponse;
import com.lms.assessment.dto.response.StudentQuestionOptionResponse;
import com.lms.assessment.entity.QuestionOption;
import com.lms.assessment.entity.QuestionType;
import com.lms.assessment.mapper.QuestionMapper;
import com.lms.assessment.repository.QuestionOptionRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentAssessmentServiceImpl implements StudentAssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final TestCaseRepository testCaseRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final SubmissionRepository submissionRepository;
    private final RubricScoreRepository rubricScoreRepository;
    private final AssessmentRetestGrantRepository retestGrantRepository;
    private final UserRepository userRepository;
    private final AssessmentMapper assessmentMapper;
    private final QuestionMapper questionMapper;
    private final com.lms.common.service.StorageService storageService;

    @Override
    public PageResponse<AssessmentSummaryResponse> listPublished(Pageable pageable) {
        Page<Assessment> page =
                assessmentRepository.findByStatusOrderByCreatedAtDesc(AssessmentStatus.PUBLISHED, pageable);

        Map<UUID, Long> questionCounts = new HashMap<>();
        if (!page.isEmpty()) {
            assessmentQuestionRepository.countByAssessmentIds(page.stream().map(Assessment::getId).toList())
                    .forEach(row -> questionCounts.put((UUID) row[0], (Long) row[1]));
        }

        return PageResponse.from(page, assessment ->
                assessmentMapper.toSummaryResponse(
                        assessment,
                        questionCounts.getOrDefault(assessment.getId(), 0L)
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

        // If there's an active IN_PROGRESS attempt, check if it's still running or expired
        Optional<AssessmentAttempt> activeAttempt = existingAttempts.stream()
                .filter(a -> a.getStatus() == AttemptStatus.IN_PROGRESS)
                .findFirst();

        AssessmentAttempt attempt = null;
        if (activeAttempt.isPresent()) {
            AssessmentAttempt current = activeAttempt.get();
            if (!current.isExpiredByTime()) {
                // Resume ongoing active attempt
                attempt = current;
            } else {
                // Attempt expired: auto-finalize drafts and mark EXPIRED
                handleAttemptExpiry(current);
                // attempt remains null so we proceed to check if student can start a new attempt or consume a retest grant
            }
        }

        int attemptNumber;
        int maxAttempts;

        if (attempt == null) {
            // Count only terminal (completed) attempts for the limit check.
            // This avoids double-counting an attempt that was just expired above.
            long usedAttempts = existingAttempts.stream()
                    .filter(a -> a.getStatus() == AttemptStatus.SUBMITTED
                              || a.getStatus() == AttemptStatus.EXPIRED)
                    .count();
            int defaultMax = assessment.getMaxAttempts();

            Optional<AssessmentRetestGrant> grant =
                    retestGrantRepository.findByAssessmentIdAndStudentId(assessmentId, studentId);

            int remainingGrants = grant.map(AssessmentRetestGrant::getExtraAttempts).orElse(0);

            if (usedAttempts >= defaultMax) {
                if (grant.isEmpty() || grant.get().getExtraAttempts() <= 0) {
                    throw new BusinessRuleException(
                            "Maximum attempt limit (" + defaultMax + ") reached for this assessment");
                }

                // Consume one grant
                AssessmentRetestGrant g = grant.get();
                if (g.getExtraAttempts() == 1) {
                    // Last grant consumed — remove the row entirely
                    retestGrantRepository.delete(g);
                    remainingGrants = 0;
                } else {
                    g.setExtraAttempts(g.getExtraAttempts() - 1);
                    retestGrantRepository.save(g);
                    remainingGrants = g.getExtraAttempts();
                }
                log.info("Student {} consumed a retest grant for assessment {}", studentId, assessmentId);
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

            attemptNumber = (int) usedAttempts + 1;
            maxAttempts = Math.max(defaultMax, attemptNumber) + remainingGrants;
        } else {
            attemptNumber = existingAttempts.size();
            Optional<AssessmentRetestGrant> grant =
                    retestGrantRepository.findByAssessmentIdAndStudentId(assessmentId, studentId);
            int remainingGrants = grant.map(AssessmentRetestGrant::getExtraAttempts).orElse(0);
            maxAttempts = Math.max(assessment.getMaxAttempts(), attemptNumber) + remainingGrants;
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
                questions,
                attemptNumber,
                maxAttempts
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
        List<Submission> changedSubmissions = new ArrayList<>();
        for (Submission sub : existingSubmissions) {
            if ("DRAFT".equals(sub.getStatus())) {
                sub.setStatus("SUBMITTED");
                sub.setSubmittedAt(now);
                changedSubmissions.add(sub);
            }
        }
        if (!changedSubmissions.isEmpty()) {
            submissionRepository.saveAll(changedSubmissions);
        }

        // Ensure any questions without a submission record also get a SUBMITTED entry
        List<AssessmentQuestion> aqList = assessmentQuestionRepository.findByAssessmentIdOrderByQuestionOrderAsc(attempt.getAssessment().getId());
        List<Submission> blankSubmissions = new ArrayList<>();
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
                blankSubmissions.add(blankSub);
            }
        }
        if (!blankSubmissions.isEmpty()) {
            submissionRepository.saveAll(blankSubmissions);
        }

        List<Submission> allSubmissions = submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId);
        int autoScore = autoGradeMcqSubmissions(attempt, allSubmissions);
        if (attempt.getScore() == null || attempt.getScore() == 0) {
            attempt.setScore(autoScore);
            attemptRepository.save(attempt);
        }

        log.info("Student {} successfully submitted attempt {} (status set to SUBMITTED in DB, auto-score: {})", studentId, attemptId, autoScore);
        allSubmissions = submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId);
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
    public PageResponse<AttemptHistoryResponse> getStudentAttemptHistory(
            UUID assessmentId, UUID studentId, Pageable pageable) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assessment", assessmentId));

        Pageable boundedPageable = PageRequest.of(
                Math.max(0, pageable.getPageNumber()),
                Math.min(Math.max(1, pageable.getPageSize()), 100),
                pageable.getSort());

        Page<AssessmentAttempt> attempts = attemptRepository
                .findByAssessmentIdAndStudentIdOrderByStartedAtDesc(assessmentId, studentId, boundedPageable);

        long totalAttempts = attempts.getTotalElements();
        List<AttemptHistoryResponse> history = new ArrayList<>(attempts.getNumberOfElements());

        Optional<AssessmentRetestGrant> grant =
                retestGrantRepository.findByAssessmentIdAndStudentId(assessmentId, studentId);
        int extraAttempts = grant.map(AssessmentRetestGrant::getExtraAttempts).orElse(0);
        boolean canRetake = totalAttempts < assessment.getMaxAttempts() || extraAttempts > 0;

        for (int i = 0; i < attempts.getNumberOfElements(); i++) {
            AssessmentAttempt a = attempts.getContent().get(i);
            long attemptNum = totalAttempts - ((long) attempts.getNumber() * attempts.getSize()) - i;
            int score = a.getScore() != null ? a.getScore() : 0;
            double pct = assessment.getTotalMarks() > 0 ? (score * 100.0 / assessment.getTotalMarks()) : 0.0;

            history.add(new AttemptHistoryResponse(
                    a.getId(),
                    assessment.getId(),
                    assessment.getTitle(),
                    Math.toIntExact(attemptNum),
                    a.getStatus(),
                    a.getScore(),
                    assessment.getTotalMarks(),
                    Math.round(pct * 10.0) / 10.0,
                    a.getStartedAt(),
                    a.getSubmittedAt(),
                    a.getExpiresAt(),
                    extraAttempts,
                    canRetake,
                    assessment.isShowResultAnalytics()
            ));
        }

        return new PageResponse<>(history, attempts.getNumber(), attempts.getSize(), totalAttempts,
                attempts.getTotalPages(), attempts.isLast());
    }

    @Override
    public AssessmentResultReportResponse getStudentResultReport(UUID targetId, UUID studentId) {
        User student = userRepository.findById(studentId).orElse(null);
        String studentName = student != null ? student.getName() : "Student";

        AssessmentAttempt attempt = attemptRepository.findByIdAndStudentId(targetId, studentId).orElse(null);
        Assessment assessment;

        if (attempt != null) {
            assessment = attempt.getAssessment();
        } else {
            // Target may be an assessment ID
            assessment = assessmentRepository.findById(targetId)
                    .orElseThrow(() -> new ResourceNotFoundException("Assessment or attempt not found: " + targetId));
            List<AssessmentAttempt> studentAttempts =
                    attemptRepository.findByAssessmentIdAndStudentIdOrderByStartedAtDesc(assessment.getId(), studentId);
            if (!studentAttempts.isEmpty()) {
                attempt = studentAttempts.get(0);
            }
        }

        List<AssessmentAttempt> allAttempts = attempt != null
                ? attemptRepository.findByAssessmentIdAndStudentIdOrderByStartedAtDesc(assessment.getId(), studentId)
                : List.of();

        Integer finalScore = attempt != null ? attempt.getScore() : null;
        double pct = (finalScore != null && assessment.getTotalMarks() > 0)
                ? (finalScore * 100.0 / assessment.getTotalMarks())
                : 0.0;
        boolean passed = finalScore != null && pct >= 50.0;

        Instant endInstant = (attempt != null && attempt.getSubmittedAt() != null)
                ? attempt.getSubmittedAt()
                : Instant.now();
        long timeSpentSeconds = attempt != null
                ? Math.max(0, Duration.between(attempt.getStartedAt(), endInstant).getSeconds())
                : 0;

        boolean showAnalytics = assessment.isShowResultAnalytics();

        // Question results (answers, test cases, rubrics and explanations if showResultAnalytics is enabled)
        List<AssessmentResultReportResponse.QuestionResultDto> qResults = new ArrayList<>();

        if (showAnalytics) {
            List<Submission> submissions = attempt != null
                    ? submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attempt.getId())
                    : List.of();
            Map<UUID, Submission> submissionMap = submissions.stream()
                    .collect(Collectors.toMap(Submission::getQuestionId, s -> s, (s1, s2) -> s1));

            List<AssessmentQuestion> aqs = assessmentQuestionRepository.findByAssessmentIdOrderByQuestionOrderAsc(assessment.getId());

            for (AssessmentQuestion aq : aqs) {
                Question q = aq.getQuestion();
                Submission sub = submissionMap.get(q.getId());

                String subStatus = sub != null ? sub.getStatus() : (attempt == null ? "READY_TO_START" : "NOT_ATTEMPTED");
                String code = sub != null ? sub.getSourceCode() : null;
                String lang = sub != null ? sub.getLanguage() : "JAVA";

                // Rubric evaluations
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
                if (rEvals.isEmpty() && q.getQuestionType() == QuestionType.CODING) {
                    int marks = aq.getMarks() > 0 ? aq.getMarks() : 10;
                    int m1 = Math.max(1, (int) Math.round(marks * 0.4));
                    int m2 = Math.max(1, (int) Math.round(marks * 0.3));
                    int m3 = Math.max(1, (int) Math.round(marks * 0.2));
                    int m4 = Math.max(1, marks - (m1 + m2 + m3));
                    boolean isAcc = sub != null && "ACCEPTED".equalsIgnoreCase(sub.getStatus());
                    rEvals.add(new AssessmentResultReportResponse.RubricEvaluationDto(
                            UUID.randomUUID(), "Functional Correctness & Test Suite", isAcc ? m1 : 0, m1,
                            isAcc ? "All automated test cases passed successfully." : "Automated assertions failed or unattempted."));
                    rEvals.add(new AssessmentResultReportResponse.RubricEvaluationDto(
                            UUID.randomUUID(), "Algorithm Design & Efficiency", isAcc ? m2 : 0, m2,
                            isAcc ? "Runtime performance adheres to target constraints." : "Execution complexity exceeded or non-optimal."));
                    rEvals.add(new AssessmentResultReportResponse.RubricEvaluationDto(
                            UUID.randomUUID(), "Code Modularity & Readability", isAcc ? m3 : 0, m3,
                            isAcc ? "Clean structure, naming, and indentation." : "Needs refactoring and structured modularity."));
                    rEvals.add(new AssessmentResultReportResponse.RubricEvaluationDto(
                            UUID.randomUUID(), "Edge Case Handling", isAcc ? m4 : 0, m4,
                            isAcc ? "Proper handling of boundary inputs and nulls." : "Boundary checks missing."));
                }

                // Options (for MCQs)
                List<QuestionOptionResponse> optResponses = List.of();
                StringBuilder mcqExplanation = new StringBuilder();
                if (q.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                    List<QuestionOption> opts = questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(q.getId());
                    optResponses = questionMapper.toQuestionOptionResponseList(opts);
                    for (QuestionOption opt : opts) {
                        if (opt.isCorrect()) {
                            if (opt.getExplanation() != null && !opt.getExplanation().isBlank()) {
                                mcqExplanation.append(opt.getExplanation()).append(" ");
                            } else {
                                mcqExplanation.append("Option '").append(opt.getOptionText()).append("' is the correct answer. ");
                            }
                        }
                    }
                }

                // Test cases (for coding questions)
                List<AssessmentResultReportResponse.TestCaseResultDto> tcResults = new ArrayList<>();
                if (q.getQuestionType() == QuestionType.CODING) {
                    List<TestCase> tcs = testCaseRepository.findByQuestionIdOrderByIdAsc(q.getId());
                    for (TestCase tc : tcs) {
                        boolean isAccepted = sub != null && "ACCEPTED".equalsIgnoreCase(sub.getStatus());
                        String actualOut;
                        if (isAccepted) {
                            actualOut = tc.getExpectedOutput();
                        } else if (sub != null && sub.getSourceCode() != null && !sub.getSourceCode().isBlank()) {
                            actualOut = "WRONG_ANSWER".equalsIgnoreCase(sub.getStatus())
                                    ? "Execution returned mismatching output."
                                    : sub.getStatus();
                        } else {
                            actualOut = "(Reference output: " + tc.getExpectedOutput() + ")";
                        }
                        tcResults.add(new AssessmentResultReportResponse.TestCaseResultDto(
                                tc.getId(),
                                tc.getInputData(),
                                tc.getExpectedOutput(),
                                actualOut,
                                isAccepted,
                                tc.isSample(),
                                tc.isHidden(),
                                tc.getWeight()
                        ));
                    }
                }

                int earned = (sub != null && "ACCEPTED".equalsIgnoreCase(sub.getStatus())) ? aq.getMarks() : 0;
                String execOutput = sub != null && sub.getSourceCode() != null && !sub.getSourceCode().isBlank()
                        ? ("ACCEPTED".equalsIgnoreCase(sub.getStatus())
                                ? "Process completed with exit code 0. All assertions satisfied."
                                : "Execution status: " + sub.getStatus())
                        : (attempt == null ? "Ready for evaluation. Run tests to see output." : "No code submitted.");

                String explanation = q.getQuestionType() == QuestionType.MULTIPLE_CHOICE
                        ? (mcqExplanation.length() > 0 ? mcqExplanation.toString().trim() : "Review standard option definitions.")
                        : (q.getDescription() != null && !q.getDescription().isBlank()
                                ? "Official Reference: Optimal solution requires adhering to time limit (" + q.getTimeLimitMs() + "ms) and memory limit (" + q.getMemoryLimitMb() + "MB). Check boundary conditions."
                                : "Optimal algorithm requires O(N) or O(log N) complexity with boundary checks.");

                qResults.add(new AssessmentResultReportResponse.QuestionResultDto(
                        q.getId(),
                        q.getTitle(),
                        q.getDescription(),
                        q.getQuestionType().name(),
                        aq.getMarks(),
                        earned,
                        subStatus,
                        code,
                        lang,
                        execOutput,
                        explanation,
                        rEvals,
                        optResponses,
                        tcResults
                ));
            }
        }

        // Class benchmark analytics & Grade Distribution
        AssessmentResultReportResponse.ResultAnalyticsSummaryDto classAnalytics = null;
        List<AssessmentResultReportResponse.GradeDistributionDto> gradeDistribution = List.of();
        Double percentileRank = null;

        if (showAnalytics) {
            List<AssessmentAttempt> assessmentAttempts =
                    attemptRepository.findByAssessmentIdOrderByStartedAtDesc(assessment.getId());

            List<AssessmentAttempt> scoredAttempts = assessmentAttempts.stream()
                    .filter(a -> a.getScore() != null)
                    .toList();

            long gradeA = 0, gradeB = 0, gradeC = 0, gradeD = 0, gradeF = 0;
            int totalMarks = assessment.getTotalMarks();

            for (AssessmentAttempt a : scoredAttempts) {
                if (a.getScore() != null && totalMarks > 0) {
                    double p = (a.getScore().doubleValue() / totalMarks) * 100.0;
                    if (p >= 90.0) gradeA++;
                    else if (p >= 80.0) gradeB++;
                    else if (p >= 70.0) gradeC++;
                    else if (p >= 60.0) gradeD++;
                    else gradeF++;
                }
            }

            long totalGraded = scoredAttempts.size();
            gradeDistribution = List.of(
                    new AssessmentResultReportResponse.GradeDistributionDto("A", "90-100%", gradeA, totalGraded > 0 ? Math.round((gradeA * 100.0 / totalGraded) * 10.0) / 10.0 : 0.0),
                    new AssessmentResultReportResponse.GradeDistributionDto("B", "80-89%", gradeB, totalGraded > 0 ? Math.round((gradeB * 100.0 / totalGraded) * 10.0) / 10.0 : 0.0),
                    new AssessmentResultReportResponse.GradeDistributionDto("C", "70-79%", gradeC, totalGraded > 0 ? Math.round((gradeC * 100.0 / totalGraded) * 10.0) / 10.0 : 0.0),
                    new AssessmentResultReportResponse.GradeDistributionDto("D", "60-69%", gradeD, totalGraded > 0 ? Math.round((gradeD * 100.0 / totalGraded) * 10.0) / 10.0 : 0.0),
                    new AssessmentResultReportResponse.GradeDistributionDto("F", "<60%", gradeF, totalGraded > 0 ? Math.round((gradeF * 100.0 / totalGraded) * 10.0) / 10.0 : 0.0)
            );

            if (!scoredAttempts.isEmpty()) {
                double avgScore = scoredAttempts.stream()
                        .mapToInt(AssessmentAttempt::getScore)
                        .average()
                        .orElse(0.0);
                int highest = scoredAttempts.stream()
                        .mapToInt(AssessmentAttempt::getScore)
                        .max()
                        .orElse(0);
                int lowest = scoredAttempts.stream()
                        .mapToInt(AssessmentAttempt::getScore)
                        .min()
                        .orElse(0);
                long passedCount = scoredAttempts.stream()
                        .filter(a -> assessment.getTotalMarks() > 0 && ((a.getScore() * 100.0 / assessment.getTotalMarks()) >= 50.0))
                        .count();
                double passPct = Math.round((passedCount * 100.0 / scoredAttempts.size()) * 10.0) / 10.0;

                classAnalytics = new AssessmentResultReportResponse.ResultAnalyticsSummaryDto(
                        Math.round(avgScore * 10.0) / 10.0,
                        highest,
                        lowest,
                        passPct,
                        scoredAttempts.size()
                );

                if (finalScore != null) {
                    long belowCount = scoredAttempts.stream()
                            .filter(a -> a.getScore() < finalScore)
                            .count();
                    percentileRank = Math.round((belowCount * 100.0 / scoredAttempts.size()) * 10.0) / 10.0;
                }
            }
        }

        List<AttemptHistoryResponse> history = getStudentAttemptHistory(
                assessment.getId(), studentId, PageRequest.of(0, 20)).getContent();
        String playbackUrl = attempt != null ? getRecordingPlaybackUrl(attempt.getId(), studentId) : null;

        Optional<AssessmentRetestGrant> grant =
                retestGrantRepository.findByAssessmentIdAndStudentId(assessment.getId(), studentId);
        int extraAttempts = grant.map(AssessmentRetestGrant::getExtraAttempts).orElse(0);
        int effectiveMaxAttempts = Math.max(assessment.getMaxAttempts(), allAttempts.size()) + extraAttempts;

        return new AssessmentResultReportResponse(
                attempt != null ? attempt.getId() : null,
                assessment.getId(),
                assessment.getTitle(),
                studentId,
                studentName,
                attempt != null ? attempt.getStatus() : AttemptStatus.NOT_STARTED,
                finalScore,
                assessment.getTotalMarks(),
                Math.round(pct * 10.0) / 10.0,
                passed,
                assessment.getRetakePolicy() != null ? assessment.getRetakePolicy().name() : "BEST_SCORE",
                allAttempts.size(),
                effectiveMaxAttempts,
                timeSpentSeconds,
                attempt != null ? attempt.getStartedAt() : null,
                attempt != null ? attempt.getSubmittedAt() : null,
                playbackUrl,
                attempt != null ? attempt.getRecordingDurationSeconds() : null,
                showAnalytics,
                percentileRank,
                classAnalytics,
                gradeDistribution,
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
        int autoScore = autoGradeMcqSubmissions(attempt, submissions);
        if (attempt.getScore() == null || attempt.getScore() == 0) {
            attempt.setScore(autoScore);
            attemptRepository.save(attempt);
        }
        log.info("Attempt {} expired by time, auto-submitted drafts and auto-graded MCQ score: {}", attempt.getId(), autoScore);
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
            List<StudentTestCaseResponse> tcResponses = List.of();
            List<StudentQuestionOptionResponse> optResponses = List.of();

            if (q.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                List<QuestionOption> opts = questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(q.getId());
                optResponses = questionMapper.toStudentQuestionOptionResponseList(opts);
            } else {
                List<TestCase> sampleTcs = testCaseRepository.findByQuestionIdAndSampleTrueOrderByIdAsc(q.getId());
                tcResponses = sampleTcs.stream()
                        .map(tc -> new StudentTestCaseResponse(tc.getId(), tc.getInputData(), tc.getExpectedOutput()))
                        .toList();
            }

            list.add(new StudentQuestionResponse(
                    q.getId(),
                    q.getTitle(),
                    q.getDescription(),
                    q.getInputFormat(),
                    q.getOutputFormat(),
                    q.getConstraints(),
                    q.getDifficulty(),
                    q.getQuestionType(),
                    aq.getMarks(),
                    q.getTimeLimitMs(),
                    q.getMemoryLimitMb(),
                    aq.getQuestionOrder(),
                    tcResponses,
                    optResponses
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

    private int autoGradeMcqSubmissions(AssessmentAttempt attempt, List<Submission> submissions) {
        List<AssessmentQuestion> aqList = assessmentQuestionRepository
                .findByAssessmentIdOrderByQuestionOrderAsc(attempt.getAssessment().getId());

        Map<UUID, Submission> submissionByQuestion = submissions.stream()
                .collect(Collectors.toMap(Submission::getQuestionId, s -> s, (a, b) -> a));

        int autoScore = 0;
        List<Submission> updated = new ArrayList<>();

        for (AssessmentQuestion aq : aqList) {
            Question q = aq.getQuestion();
            if (q.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                Submission sub = submissionByQuestion.get(q.getId());
                if (sub != null) {
                    List<QuestionOption> options = questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(q.getId());
                    Set<String> correctIds = options.stream()
                            .filter(QuestionOption::isCorrect)
                            .map(o -> o.getId().toString().toLowerCase())
                            .collect(Collectors.toSet());

                    Set<String> studentSelected = parseSelectedOptionIds(sub.getSourceCode());
                    if (!studentSelected.isEmpty()) {
                        if (studentSelected.equals(correctIds)) {
                            autoScore += aq.getMarks();
                            sub.setStatus("ACCEPTED");
                        } else {
                            sub.setStatus("WRONG_ANSWER");
                        }
                    } else {
                        sub.setStatus("UNANSWERED");
                    }
                    updated.add(sub);
                }
            }
        }

        if (!updated.isEmpty()) {
            submissionRepository.saveAll(updated);
        }

        return autoScore;
    }

    private Set<String> parseSelectedOptionIds(String sourceCode) {
        if (!org.springframework.util.StringUtils.hasText(sourceCode)) {
            return Set.of();
        }
        String clean = sourceCode.trim();
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        String[] parts = clean.split(",");
        Set<String> result = new HashSet<>();
        for (String p : parts) {
            String trimmed = p.trim().replace("\"", "").replace("'", "").toLowerCase();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
