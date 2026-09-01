package com.lms.assessment.service;

import com.lms.assessment.dto.request.GradeSubmissionRequest;
import com.lms.assessment.dto.request.RubricScoreRequest;
import com.lms.assessment.dto.response.AttemptDetailResponse;
import com.lms.assessment.dto.response.SubmissionResponse;
import com.lms.assessment.entity.AssessmentAttempt;
import com.lms.assessment.entity.RubricCriterion;
import com.lms.assessment.entity.RubricScore;
import com.lms.assessment.entity.Submission;
import com.lms.assessment.repository.AssessmentAttemptRepository;
import com.lms.assessment.repository.RubricCriterionRepository;
import com.lms.assessment.repository.RubricScoreRepository;
import com.lms.assessment.repository.SubmissionRepository;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradingServiceImpl implements GradingService {

    private final SubmissionRepository submissionRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricScoreRepository rubricScoreRepository;
    private final StudentAssessmentService studentAssessmentService;

    @Override
    public PageResponse<SubmissionResponse> getPendingSubmissions(Pageable pageable) {
        Page<Submission> page = submissionRepository.findByStatusOrderBySubmittedAtDesc("SUBMITTED", pageable);
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
    public AttemptDetailResponse gradeSubmission(UUID attemptId, GradeSubmissionRequest request, UUID evaluatorId) {
        AssessmentAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> ResourceNotFoundException.of("AssessmentAttempt", attemptId));

        Submission submission = submissionRepository.findById(request.submissionId())
                .orElseThrow(() -> ResourceNotFoundException.of("Submission", request.submissionId()));

        if (request.status() != null) {
            submission.setStatus(request.status());
        } else {
            submission.setStatus("GRADED");
        }
        submissionRepository.save(submission);

        // Clear existing rubric scores for this submission if updating grade
        rubricScoreRepository.deleteBySubmissionId(submission.getId());

        int totalRubricScore = 0;
        if (request.rubricScores() != null && !request.rubricScores().isEmpty()) {
            for (RubricScoreRequest rsReq : request.rubricScores()) {
                RubricCriterion criterion = rubricCriterionRepository.findById(rsReq.criterionId())
                        .orElseThrow(() -> ResourceNotFoundException.of("RubricCriterion", rsReq.criterionId()));

                RubricScore score = RubricScore.builder()
                        .attempt(attempt)
                        .submission(submission)
                        .criterion(criterion)
                        .score(rsReq.score())
                        .feedback(rsReq.feedback())
                        .evaluatorId(evaluatorId)
                        .build();

                rubricScoreRepository.save(score);
                totalRubricScore += (int) Math.round(rsReq.score() * criterion.getWeight());
            }
        }

        // Recalculate total attempt score
        int manualScore = request.manualScore() != null ? request.manualScore() : totalRubricScore;
        int currentAttemptScore = attempt.getScore() != null ? attempt.getScore() : 0;
        attempt.setScore(currentAttemptScore + manualScore);
        attemptRepository.save(attempt);

        log.info("Evaluator {} graded submission {} for attempt {} with score {}", evaluatorId, submission.getId(), attemptId, manualScore);

        return studentAssessmentService.getAttemptDetail(attemptId, attempt.getStudentId());
    }
}
