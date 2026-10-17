package com.lms.assessment.service;

import com.lms.assessment.dto.request.GradeSubmissionRequest;
import com.lms.assessment.dto.request.RubricScoreRequest;
import com.lms.assessment.dto.response.AttemptDetailResponse;
import com.lms.assessment.dto.response.SubmissionResponse;
import com.lms.assessment.entity.AssessmentAttempt;
import com.lms.assessment.entity.AssessmentQuestion;
import com.lms.assessment.entity.QuestionType;
import com.lms.assessment.entity.RubricCriterion;
import com.lms.assessment.entity.RubricScore;
import com.lms.assessment.entity.Submission;
import com.lms.assessment.repository.AssessmentAttemptRepository;
import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.RubricCriterionRepository;
import com.lms.assessment.repository.RubricScoreRepository;
import com.lms.assessment.repository.SubmissionRepository;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.PageResponse;
import com.lms.security.authentication.AuthenticationService;
import com.lms.security.authentication.LmsUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradingServiceImpl implements GradingService {

    private static final String PENDING = "SUBMITTED";

    private final SubmissionRepository submissionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricScoreRepository rubricScoreRepository;
    private final StudentAssessmentService studentAssessmentService;

    @Override
    public PageResponse<SubmissionResponse> getPendingSubmissions(Pageable pageable) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        Page<Submission> page = GradingAccess.isAdministrator(principal)
                ? submissionRepository.findByStatusOrderBySubmittedAtDesc(PENDING, pageable)
                : submissionRepository.findByStatusAndAttempt_Assessment_CreatedByOrderBySubmittedAtDesc(
                        PENDING, principal.getUserId(), pageable);
        return PageResponse.from(page, s -> new SubmissionResponse(
                s.getId(),
                s.getAttempt().getId(),
                s.getQuestionId(),
                s.getLanguage(),
                s.getSourceCode(),
                s.getStatus(),
                s.getSubmittedAt()
        ));
    }

    @Override
    @Transactional
    public AttemptDetailResponse gradeSubmission(UUID attemptId, GradeSubmissionRequest request,
                                                 UUID evaluatorId, boolean mayGradeAnyAssessment) {
        AssessmentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> ResourceNotFoundException.of("AssessmentAttempt", attemptId));

        Submission submission = submissionRepository.findById(request.submissionId())
                .orElseThrow(() -> ResourceNotFoundException.of("Submission", request.submissionId()));

        if (!submission.getAttempt().getId().equals(attemptId)) {
            throw new BusinessRuleException("Submission does not belong to the assessment attempt being graded");
        }
        if (!mayGradeAnyAssessment && !evaluatorId.equals(attempt.getAssessment().getCreatedBy())) {
            throw new AccessDeniedException("You can only grade assessments you created");
        }

        Map<UUID, AssessmentQuestion> questions = assessmentQuestions(attempt);
        AssessmentQuestion graded = questions.get(submission.getQuestionId());
        int questionMarks = graded != null ? graded.getMarks() : attempt.getAssessment().getTotalMarks();

        rubricScoreRepository.deleteBySubmissionId(submission.getId());
        int rubricTotal = 0;
        if (request.rubricScores() != null) {
            for (RubricScoreRequest rsReq : request.rubricScores()) {
                RubricCriterion criterion = rubricCriterionRepository.findById(rsReq.criterionId())
                        .orElseThrow(() -> ResourceNotFoundException.of("RubricCriterion", rsReq.criterionId()));
                rubricScoreRepository.save(RubricScore.builder()
                        .attempt(attempt)
                        .submission(submission)
                        .criterion(criterion)
                        .score(rsReq.score())
                        .feedback(rsReq.feedback())
                        .evaluatorId(evaluatorId)
                        .build());
                rubricTotal += (int) Math.round(rsReq.score() * criterion.getWeight());
            }
        }

        // The score is for this answer; the attempt total is recomputed from every answer.
        int score = request.manualScore() != null ? request.manualScore() : rubricTotal;
        if (score < 0 || score > questionMarks) {
            throw new BusinessRuleException("Score for this question must be between 0 and " + questionMarks);
        }
        submission.setScore(score);
        submission.setStatus(request.status() != null ? request.status() : "GRADED");
        submissionRepository.save(submission);

        attempt.setScore(attemptTotal(attempt, questions));
        attemptRepository.save(attempt);

        log.info("Evaluator {} graded submission {} of attempt {}: {}/{} (attempt total {})", evaluatorId,
                submission.getId(), attemptId, score, questionMarks, attempt.getScore());

        return studentAssessmentService.getAttemptDetail(attemptId, attempt.getStudentId());
    }

    private Map<UUID, AssessmentQuestion> assessmentQuestions(AssessmentAttempt attempt) {
        return assessmentQuestionRepository
                .findByAssessmentIdOrderByQuestionOrderAsc(attempt.getAssessment().getId()).stream()
                .collect(Collectors.toMap(aq -> aq.getQuestion().getId(), Function.identity(), (a, b) -> a));
    }

    /**
     * Sum of every answer's score. Answers graded before scores were stored per
     * answer have none: a correct multiple-choice answer then counts its marks.
     */
    private int attemptTotal(AssessmentAttempt attempt, Map<UUID, AssessmentQuestion> questions) {
        List<Submission> answers = submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attempt.getId());
        int total = 0;
        for (Submission answer : answers) {
            if (answer.getScore() != null) {
                total += answer.getScore();
                continue;
            }
            AssessmentQuestion aq = questions.get(answer.getQuestionId());
            if (aq != null && aq.getQuestion().getQuestionType() == QuestionType.MULTIPLE_CHOICE
                    && "ACCEPTED".equalsIgnoreCase(answer.getStatus())) {
                total += aq.getMarks();
            }
        }
        return Math.min(total, attempt.getAssessment().getTotalMarks());
    }
}
