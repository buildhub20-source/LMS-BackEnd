package com.lms.assessment.service;

import com.lms.assessment.dto.request.CreateQuestionRequest;
import com.lms.assessment.dto.request.CreateTestCaseRequest;
import com.lms.assessment.dto.response.QuestionResponse;
import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentQuestion;
import com.lms.assessment.entity.AssessmentStatus;
import com.lms.assessment.entity.Difficulty;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.QuestionType;
import com.lms.assessment.entity.TestCase;
import com.lms.assessment.mapper.QuestionMapper;
import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.AssessmentRepository;
import com.lms.assessment.repository.QuestionRepository;
import com.lms.assessment.repository.TestCaseRepository;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminQuestionServiceImpl")
class AdminQuestionServiceTest {

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private TestCaseRepository testCaseRepository;
    @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
    @Mock private QuestionMapper questionMapper;

    @InjectMocks
    private AdminQuestionServiceImpl service;

    private final UUID assessmentId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();

    private Assessment draftAssessment() {
        return Assessment.builder()
                .id(assessmentId)
                .title("Java Fundamentals")
                .status(AssessmentStatus.DRAFT)
                .totalMarks(0)
                .createdBy(UUID.randomUUID())
                .build();
    }

    private Question question() {
        return Question.builder()
                .id(questionId)
                .title("Two Sum")
                .description("Find indices")
                .difficulty(Difficulty.EASY)
                .questionType(QuestionType.CODING)
                .marks(20)
                .build();
    }

    @Nested
    @DisplayName("addQuestion()")
    class AddQuestion {

        @Test
        @DisplayName("adds question with test cases to draft assessment and recalculates totalMarks")
        void addsQuestionSuccessfully() {
            Assessment assessment = draftAssessment();
            Question q = question();

            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
            when(questionRepository.save(any())).thenReturn(q);
            when(testCaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(assessmentQuestionRepository.countByAssessmentId(assessmentId)).thenReturn(0L);

            CreateTestCaseRequest tc = new CreateTestCaseRequest("1 2", "3", true, false, 1);
            CreateQuestionRequest request = new CreateQuestionRequest(
                    "Two Sum", "Find indices", "int[]", "int[]", "N<=1000",
                    Difficulty.EASY, QuestionType.CODING, 20, 2000, 256, List.of(tc));

            when(assessmentQuestionRepository.findByAssessmentIdOrderByQuestionOrderAsc(assessmentId))
                    .thenReturn(List.of(AssessmentQuestion.builder().assessment(assessment).question(q).marks(20).build()));

            service.addQuestion(assessmentId, request);

            verify(questionRepository).save(any(Question.class));
            verify(assessmentQuestionRepository).save(any(AssessmentQuestion.class));
            assertThat(assessment.getTotalMarks()).isEqualTo(20);
        }

        @Test
        @DisplayName("refuses to add question to published assessment")
        void refusesOnPublished() {
            Assessment assessment = draftAssessment();
            assessment.publish();
            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

            CreateQuestionRequest request = new CreateQuestionRequest(
                    "Two Sum", "Find indices", null, null, null,
                    Difficulty.EASY, QuestionType.CODING, 20, 2000, 256, List.of());

            assertThatThrownBy(() -> service.addQuestion(assessmentId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("PUBLISHED");
        }
    }

    @Nested
    @DisplayName("removeQuestionFromAssessment()")
    class RemoveQuestion {

        @Test
        @DisplayName("removes question and recalculates remaining marks")
        void removesQuestion() {
            Assessment assessment = draftAssessment();
            Question q = question();
            AssessmentQuestion aq = AssessmentQuestion.builder()
                    .assessment(assessment)
                    .question(q)
                    .questionOrder(0)
                    .marks(20)
                    .build();

            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
            when(assessmentQuestionRepository.findByAssessmentIdOrderByQuestionOrderAsc(assessmentId))
                    .thenReturn(List.of(aq));

            service.removeQuestionFromAssessment(assessmentId, questionId);

            verify(assessmentQuestionRepository).delete(aq);
        }
    }
}
