package com.lms.assessment.service;

import com.lms.assessment.dto.request.GradeSubmissionRequest;
import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentAttempt;
import com.lms.assessment.entity.Submission;
import com.lms.assessment.repository.AssessmentAttemptRepository;
import com.lms.assessment.repository.RubricCriterionRepository;
import com.lms.assessment.repository.RubricScoreRepository;
import com.lms.assessment.repository.SubmissionRepository;
import com.lms.common.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GradingServiceImpl")
class GradingServiceImplTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private AssessmentAttemptRepository attemptRepository;
    @Mock private RubricCriterionRepository rubricCriterionRepository;
    @Mock private RubricScoreRepository rubricScoreRepository;
    @Mock private StudentAssessmentService studentAssessmentService;

    @InjectMocks private GradingServiceImpl service;

    private final UUID attemptId = UUID.randomUUID();
    private final UUID submissionId = UUID.randomUUID();
    private final UUID evaluatorId = UUID.randomUUID();

    @Test
    @DisplayName("re-grading replaces the attempt score instead of accumulating it")
    void regradingReplacesScore() {
        AssessmentAttempt attempt = attempt(100, evaluatorId);
        attempt.setScore(80);
        Submission submission = Submission.builder().id(submissionId).attempt(attempt).build();
        GradeSubmissionRequest request = new GradeSubmissionRequest(submissionId, 75, null, null, null);

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        service.gradeSubmission(attemptId, request, evaluatorId);

        ArgumentCaptor<AssessmentAttempt> savedAttempt = ArgumentCaptor.forClass(AssessmentAttempt.class);
        verify(attemptRepository).save(savedAttempt.capture());
        assertThat(savedAttempt.getValue().getScore()).isEqualTo(75);
    }

    @Test
    @DisplayName("rejects a manual score outside the assessment range before mutating the submission")
    void rejectsOutOfRangeScore() {
        AssessmentAttempt attempt = attempt(100, evaluatorId);
        Submission submission = Submission.builder().id(submissionId).attempt(attempt).build();
        GradeSubmissionRequest request = new GradeSubmissionRequest(submissionId, 101, null, null, null);

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        assertThatThrownBy(() -> service.gradeSubmission(attemptId, request, evaluatorId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("between 0 and 100");

        verify(submissionRepository, never()).save(submission);
        verify(attemptRepository, never()).save(attempt);
    }

    @Test
    @DisplayName("rejects an evaluator who does not own the assessment")
    void rejectsNonOwner() {
        AssessmentAttempt attempt = attempt(100, UUID.randomUUID());
        Submission submission = Submission.builder().id(submissionId).attempt(attempt).build();
        GradeSubmissionRequest request = new GradeSubmissionRequest(submissionId, 50, null, null, null);

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(submissionRepository.findById(submissionId)).thenReturn(Optional.of(submission));

        assertThatThrownBy(() -> service.gradeSubmission(attemptId, request, evaluatorId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not authorized");

        verify(submissionRepository, never()).save(submission);
        verify(attemptRepository, never()).save(attempt);
    }

    private AssessmentAttempt attempt(int totalMarks, UUID ownerId) {
        Assessment assessment = Assessment.builder()
                .id(UUID.randomUUID())
                .totalMarks(totalMarks)
                .createdBy(ownerId)
                .build();
        return AssessmentAttempt.builder()
                .id(attemptId)
                .assessment(assessment)
                .studentId(UUID.randomUUID())
                .startedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
