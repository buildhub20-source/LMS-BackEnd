package com.lms.assessment.service;

import com.lms.assessment.dto.request.GradeSubmissionRequest;
import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentAttempt;
import com.lms.assessment.entity.AssessmentQuestion;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.QuestionType;
import com.lms.assessment.entity.Submission;
import com.lms.assessment.repository.AssessmentAttemptRepository;
import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.RubricCriterionRepository;
import com.lms.assessment.repository.RubricScoreRepository;
import com.lms.assessment.repository.SubmissionRepository;
import com.lms.common.exception.BusinessRuleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GradingServiceImpl")
class GradingServiceImplTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private AssessmentAttemptRepository attemptRepository;
    @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
    @Mock private RubricCriterionRepository rubricCriterionRepository;
    @Mock private RubricScoreRepository rubricScoreRepository;
    @Mock private StudentAssessmentService studentAssessmentService;

    @InjectMocks private GradingServiceImpl service;

    private final UUID attemptId = UUID.randomUUID();
    private final UUID evaluatorId = UUID.randomUUID();
    private final Question mcq = question(QuestionType.MULTIPLE_CHOICE);
    private final Question codingA = question(QuestionType.CODING);
    private final Question codingB = question(QuestionType.CODING);

    private AssessmentAttempt attempt;
    private Submission mcqAnswer;
    private Submission answerA;
    private Submission answerB;

    @BeforeEach
    void setUp() {
        attempt = attempt(40, evaluatorId);
        mcqAnswer = answer(mcq, "ACCEPTED");
        answerA = answer(codingA, "SUBMITTED");
        answerB = answer(codingB, "SUBMITTED");

        when(attemptRepository.findById(attemptId)).thenReturn(Optional.of(attempt));
        when(assessmentQuestionRepository.findByAssessmentIdOrderByQuestionOrderAsc(any()))
                .thenReturn(List.of(link(mcq, 10), link(codingA, 15), link(codingB, 15)));
        when(submissionRepository.findByAttemptIdOrderByQuestionIdAsc(attemptId))
                .thenReturn(List.of(mcqAnswer, answerA, answerB));
        for (Submission s : List.of(mcqAnswer, answerA, answerB)) {
            when(submissionRepository.findById(s.getId())).thenReturn(Optional.of(s));
        }
    }

    @Test
    @DisplayName("grading a second coding answer adds to the total instead of overwriting it")
    void answersAccumulateIntoTheAttemptTotal() {
        service.gradeSubmission(attemptId, grade(answerA, 12), evaluatorId);
        service.gradeSubmission(attemptId, grade(answerB, 9), evaluatorId);

        assertThat(answerA.getScore()).isEqualTo(12);
        assertThat(answerB.getScore()).isEqualTo(9);
        assertThat(attempt.getScore()).isEqualTo(10 + 12 + 9);
    }

    @Test
    @DisplayName("re-grading an answer replaces that answer's score")
    void regradingReplacesTheAnswersScore() {
        service.gradeSubmission(attemptId, grade(answerA, 12), evaluatorId);
        service.gradeSubmission(attemptId, grade(answerA, 7), evaluatorId);

        ArgumentCaptor<AssessmentAttempt> saved = ArgumentCaptor.forClass(AssessmentAttempt.class);
        verify(attemptRepository, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getValue().getScore()).isEqualTo(10 + 7);
    }

    @Test
    @DisplayName("rejects a score above the question's marks before saving anything")
    void rejectsScoreAboveTheQuestionsMarks() {
        assertThatThrownBy(() -> service.gradeSubmission(attemptId, grade(answerA, 16), evaluatorId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("between 0 and 15");

        verify(submissionRepository, never()).save(answerA);
        verify(attemptRepository, never()).save(attempt);
    }

    @Test
    @DisplayName("an instructor cannot grade another instructor's assessment")
    void rejectsNonOwner() {
        attempt.getAssessment().setCreatedBy(UUID.randomUUID());

        assertThatThrownBy(() -> service.gradeSubmission(attemptId, grade(answerA, 5), evaluatorId))
                .isInstanceOf(AccessDeniedException.class);

        verify(submissionRepository, never()).save(answerA);
    }

    @Test
    @DisplayName("an administrator may grade any assessment")
    void administratorMayGradeAnyAssessment() {
        attempt.getAssessment().setCreatedBy(UUID.randomUUID());

        service.gradeSubmission(attemptId, grade(answerA, 5), evaluatorId, true);

        assertThat(answerA.getScore()).isEqualTo(5);
    }

    private GradeSubmissionRequest grade(Submission answer, int score) {
        return new GradeSubmissionRequest(answer.getId(), score, null, null, null);
    }

    private Submission answer(Question question, String status) {
        return Submission.builder().id(UUID.randomUUID()).attempt(attempt).questionId(question.getId())
                .status(status).build();
    }

    private static Question question(QuestionType type) {
        Question q = new Question();
        q.setId(UUID.randomUUID());
        q.setQuestionType(type);
        return q;
    }

    private static AssessmentQuestion link(Question question, int marks) {
        AssessmentQuestion aq = new AssessmentQuestion();
        aq.setQuestion(question);
        aq.setMarks(marks);
        return aq;
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
