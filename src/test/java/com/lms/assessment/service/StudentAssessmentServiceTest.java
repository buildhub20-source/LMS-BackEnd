package com.lms.assessment.service;

import com.lms.assessment.dto.request.SaveSubmissionRequest;
import com.lms.assessment.dto.response.AttemptDetailResponse;
import com.lms.assessment.dto.response.StartAttemptResponse;
import com.lms.assessment.dto.response.SubmissionResponse;
import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentAttempt;
import com.lms.assessment.entity.AssessmentStatus;
import com.lms.assessment.entity.AttemptStatus;
import com.lms.assessment.entity.Submission;
import com.lms.assessment.mapper.AssessmentMapper;
import com.lms.assessment.repository.AssessmentAttemptRepository;
import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.AssessmentRepository;
import com.lms.assessment.repository.SubmissionRepository;
import com.lms.assessment.repository.TestCaseRepository;
import com.lms.common.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StudentAssessmentServiceImpl")
class StudentAssessmentServiceTest {

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private AssessmentAttemptRepository attemptRepository;
    @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
    @Mock private TestCaseRepository testCaseRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private AssessmentMapper assessmentMapper;

    @InjectMocks
    private StudentAssessmentServiceImpl service;

    private final UUID assessmentId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID attemptId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();

    private Assessment publishedAssessment() {
        return Assessment.builder()
                .id(assessmentId)
                .title("Java Fundamentals")
                .status(AssessmentStatus.PUBLISHED)
                .durationMinutes(60)
                .maxAttempts(2)
                .build();
    }

    private AssessmentAttempt activeAttempt(Assessment assessment) {
        return AssessmentAttempt.builder()
                .id(attemptId)
                .assessment(assessment)
                .studentId(studentId)
                .startedAt(Instant.now())
                .expiresAt(Instant.now().plus(60, ChronoUnit.MINUTES))
                .status(AttemptStatus.IN_PROGRESS)
                .build();
    }

    @Nested
    @DisplayName("startAttempt()")
    class StartAttempt {

        @Test
        @DisplayName("starts a new attempt when limit is not reached")
        void startsNewAttempt() {
            Assessment assessment = publishedAssessment();
            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
            when(attemptRepository.findByAssessmentIdAndStudentIdOrderByStartedAtDesc(assessmentId, studentId))
                    .thenReturn(List.of());
            when(attemptRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            StartAttemptResponse response = service.startAttempt(assessmentId, studentId);

            assertThat(response.assessmentId()).isEqualTo(assessmentId);
            assertThat(response.status()).isEqualTo(AttemptStatus.IN_PROGRESS);
            verify(attemptRepository).save(any(AssessmentAttempt.class));
        }

        @Test
        @DisplayName("resumes active attempt if student reopens before expiry")
        void resumesActiveAttempt() {
            Assessment assessment = publishedAssessment();
            AssessmentAttempt attempt = activeAttempt(assessment);

            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
            when(attemptRepository.findByAssessmentIdAndStudentIdOrderByStartedAtDesc(assessmentId, studentId))
                    .thenReturn(List.of(attempt));

            StartAttemptResponse response = service.startAttempt(assessmentId, studentId);

            assertThat(response.attemptId()).isEqualTo(attemptId);
        }

        @Test
        @DisplayName("throws exception when max attempts limit reached")
        void refusesWhenMaxAttemptsReached() {
            Assessment assessment = publishedAssessment();
            AssessmentAttempt past1 = activeAttempt(assessment);
            past1.setStatus(AttemptStatus.SUBMITTED);
            AssessmentAttempt past2 = activeAttempt(assessment);
            past2.setStatus(AttemptStatus.SUBMITTED);

            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
            when(attemptRepository.findByAssessmentIdAndStudentIdOrderByStartedAtDesc(assessmentId, studentId))
                    .thenReturn(List.of(past1, past2));

            assertThatThrownBy(() -> service.startAttempt(assessmentId, studentId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("limit");
        }
    }

    @Nested
    @DisplayName("saveSubmission()")
    class SaveSubmission {

        @Test
        @DisplayName("autosaves code draft for active attempt")
        void savesCodeDraft() {
            Assessment assessment = publishedAssessment();
            AssessmentAttempt attempt = activeAttempt(assessment);

            when(attemptRepository.findByIdAndStudentId(attemptId, studentId)).thenReturn(Optional.of(attempt));
            when(submissionRepository.findByAttemptIdAndQuestionId(attemptId, questionId)).thenReturn(Optional.empty());

            Submission savedSub = Submission.builder()
                    .id(UUID.randomUUID())
                    .attempt(attempt)
                    .questionId(questionId)
                    .studentId(studentId)
                    .language("JAVA")
                    .sourceCode("class Solution {}")
                    .status("DRAFT")
                    .build();

            when(submissionRepository.save(any())).thenReturn(savedSub);

            SaveSubmissionRequest req = new SaveSubmissionRequest(questionId, "JAVA", "class Solution {}");
            SubmissionResponse res = service.saveSubmission(attemptId, studentId, req);

            assertThat(res.sourceCode()).isEqualTo("class Solution {}");
        }
    }

    @Nested
    @DisplayName("submitAttempt()")
    class SubmitAttempt {

        @Test
        @DisplayName("marks attempt as SUBMITTED and updates code drafts")
        void submitsAttempt() {
            Assessment assessment = publishedAssessment();
            AssessmentAttempt attempt = activeAttempt(assessment);

            when(attemptRepository.findByIdAndStudentId(attemptId, studentId)).thenReturn(Optional.of(attempt));
            when(submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId)).thenReturn(List.of());

            AttemptDetailResponse res = service.submitAttempt(attemptId, studentId);

            assertThat(res.status()).isEqualTo(AttemptStatus.SUBMITTED);
            verify(attemptRepository).save(attempt);
        }
    }
}
