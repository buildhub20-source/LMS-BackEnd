package com.lms.assessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.assessment.ai.AiGradingClient;
import com.lms.assessment.ai.AiGradingException;
import com.lms.assessment.dto.response.AiGradingSuggestionResponse;
import com.lms.assessment.entity.AiGradingResult;
import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentAttempt;
import com.lms.assessment.entity.AssessmentQuestion;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.QuestionType;
import com.lms.assessment.entity.Rubric;
import com.lms.assessment.entity.RubricCriterion;
import com.lms.assessment.entity.Submission;
import com.lms.assessment.entity.TestCase;
import com.lms.assessment.repository.AiGradingResultRepository;
import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.QuestionRepository;
import com.lms.assessment.repository.RubricCriterionRepository;
import com.lms.assessment.repository.SubmissionRepository;
import com.lms.assessment.repository.TestCaseRepository;
import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ErrorCode;
import com.lms.security.authentication.LmsUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiGradingServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SubmissionRepository submissions = mock(SubmissionRepository.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final AssessmentQuestionRepository assessmentQuestions = mock(AssessmentQuestionRepository.class);
    private final TestCaseRepository testCases = mock(TestCaseRepository.class);
    private final RubricCriterionRepository criteria = mock(RubricCriterionRepository.class);
    private final AiGradingResultRepository results = mock(AiGradingResultRepository.class);
    private final AiGradingClient client = mock(AiGradingClient.class);
    private final AiGradingService service = new AiGradingService(submissions, questions, assessmentQuestions,
            testCases, criteria, results, client, objectMapper, mock(PlatformTransactionManager.class));

    private final UUID instructorId = UUID.randomUUID();
    private Question question;
    private Submission submission;
    private Assessment assessment;

    @BeforeEach
    void setUp() {
        assessment = Assessment.builder().id(UUID.randomUUID()).totalMarks(30).createdBy(instructorId).build();
        AssessmentAttempt attempt = AssessmentAttempt.builder().id(UUID.randomUUID()).assessment(assessment)
                .studentId(UUID.randomUUID()).startedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        question = new Question();
        question.setId(UUID.randomUUID());
        question.setTitle("Sum of two numbers");
        question.setDescription("Read two integers and print their sum.");
        question.setQuestionType(QuestionType.CODING);
        question.setMarks(10);
        submission = Submission.builder().id(UUID.randomUUID()).attempt(attempt).questionId(question.getId())
                .language("PYTHON").sourceCode("a, b = map(int, input().split())\nprint(a + b)")
                .status("SUBMITTED").build();

        TestCase visible = testCase("1 2", "3", false);
        TestCase hidden = testCase("-5 5", "0", true);
        AssessmentQuestion link = new AssessmentQuestion();
        link.setQuestion(question);
        link.setMarks(15);

        when(submissions.findById(submission.getId())).thenReturn(Optional.of(submission));
        when(questions.findById(question.getId())).thenReturn(Optional.of(question));
        when(testCases.findByQuestionIdOrderByIdAsc(question.getId())).thenReturn(List.of(visible, hidden));
        when(assessmentQuestions.findByAssessmentIdAndQuestionId(assessment.getId(), question.getId()))
                .thenReturn(Optional.of(link));
        when(results.save(any())).thenAnswer(inv -> {
            AiGradingResult r = inv.getArgument(0);
            r.setId(UUID.randomUUID());
            return r;
        });
        when(client.isEnabled()).thenReturn(true);
        signIn(instructorId, "INSTRUCTOR");
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendsTheAnswerAndStoresTheSuggestion() throws Exception {
        when(client.evaluate(any())).thenReturn(objectMapper.readTree("""
                {"status":"SUGGESTED","score":12.5,"maxScore":15,"testsPassed":1,"testsTotal":2,
                 "testResults":[],"rubric":[],"feedback":"Handles positives only.","suggestions":[],
                 "usage":{"inputTokens":1800,"outputTokens":420}}"""));

        AiGradingSuggestionResponse response = service.suggest(submission.getId(), null);

        ArgumentCaptor<AiGradingClient.GradeRequest> sent = ArgumentCaptor.forClass(AiGradingClient.GradeRequest.class);
        verify(client).evaluate(sent.capture());
        assertThat(sent.getValue().question().marks()).isEqualTo(15);
        assertThat(sent.getValue().testCases()).hasSize(2).anyMatch(AiGradingClient.TestCase::hidden);
        assertThat(sent.getValue().submission().language()).isEqualTo("PYTHON");

        assertThat(response.status()).isEqualTo("SUGGESTED");
        assertThat(response.score()).isEqualByComparingTo(new BigDecimal("12.5"));
        assertThat(response.result().path("feedback").asText()).isEqualTo("Handles positives only.");
        assertThat(response.result().has("usage")).isFalse();

        ArgumentCaptor<AiGradingResult> stored = ArgumentCaptor.forClass(AiGradingResult.class);
        verify(results).save(stored.capture());
        assertThat(stored.getValue().getInputTokens()).isEqualTo(1800);
        assertThat(stored.getValue().getRequestedBy()).isEqualTo(instructorId);
    }

    @Test
    void gradesAgainstTheChosenRubric() throws Exception {
        UUID rubricId = UUID.randomUUID();
        RubricCriterion correctness = RubricCriterion.builder().id(UUID.randomUUID())
                .rubric(Rubric.builder().id(rubricId).build()).criterionName("Correctness").maxPoints(10).build();
        when(criteria.findByRubricIdOrderByIdAsc(rubricId)).thenReturn(List.of(correctness));
        when(client.evaluate(any())).thenReturn(objectMapper.readTree("{\"score\":10}"));

        service.suggest(submission.getId(), rubricId);

        ArgumentCaptor<AiGradingClient.GradeRequest> sent = ArgumentCaptor.forClass(AiGradingClient.GradeRequest.class);
        verify(client).evaluate(sent.capture());
        assertThat(sent.getValue().rubric()).singleElement()
                .satisfies(c -> assertThat(c.id()).isEqualTo(correctness.getId().toString()));
    }

    @Test
    void anotherInstructorCannotRequestASuggestion() {
        signIn(UUID.randomUUID(), "INSTRUCTOR");

        assertThatThrownBy(() -> service.suggest(submission.getId(), null)).isInstanceOf(AccessDeniedException.class);
        verify(client, never()).evaluate(any());
    }

    @Test
    void anAdministratorCanRequestASuggestion() throws Exception {
        signIn(UUID.randomUUID(), "ADMIN");
        when(client.evaluate(any())).thenReturn(objectMapper.readTree("{\"score\":5}"));

        assertThat(service.suggest(submission.getId(), null).status()).isEqualTo("SUGGESTED");
    }

    @Test
    void refusesAQuestionWithoutTestCases() {
        when(testCases.findByQuestionIdOrderByIdAsc(question.getId())).thenReturn(List.of());

        assertThatThrownBy(() -> service.suggest(submission.getId(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("test case");
        verify(client, never()).evaluate(any());
    }

    @Test
    void refusesAnEmptyAnswer() {
        submission.setSourceCode("  ");

        assertThatThrownBy(() -> service.suggest(submission.getId(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("did not submit any code");
    }

    @Test
    void refusesMultipleChoiceQuestions() {
        question.setQuestionType(QuestionType.MULTIPLE_CHOICE);

        assertThatThrownBy(() -> service.suggest(submission.getId(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("coding questions");
    }

    @Test
    void saysSoWhenAiGradingIsNotEnabled() {
        when(client.isEnabled()).thenReturn(false);

        assertThatThrownBy(() -> service.suggest(submission.getId(), null))
                .isInstanceOf(ApplicationException.class)
                .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE));
        verify(results, never()).save(any());
    }

    @Test
    void recordsARefusalAndReportsItAsNotGradable() {
        when(client.evaluate(any())).thenThrow(
                new AiGradingException("GRADING_REFUSED", "The submission could not be graded automatically", false));

        assertThatThrownBy(() -> service.suggest(submission.getId(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("could not be graded");

        ArgumentCaptor<AiGradingResult> stored = ArgumentCaptor.forClass(AiGradingResult.class);
        verify(results).save(stored.capture());
        assertThat(stored.getValue().getStatus()).isEqualTo(AiGradingResult.FAILED);
        assertThat(stored.getValue().getErrorCode()).isEqualTo("GRADING_REFUSED");
    }

    @Test
    void reportsAnUnreachableServiceAsTemporarilyUnavailable() {
        when(client.evaluate(any())).thenThrow(
                new AiGradingException("AI_UNAVAILABLE", "The AI service could not be reached", true));

        assertThatThrownBy(() -> service.suggest(submission.getId(), null))
                .isInstanceOf(ApplicationException.class)
                .satisfies(ex -> assertThat(((ApplicationException) ex).getErrorCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE));
    }

    private TestCase testCase(String input, String expected, boolean hidden) {
        TestCase tc = new TestCase();
        tc.setId(UUID.randomUUID());
        tc.setQuestion(question);
        tc.setInputData(input);
        tc.setExpectedOutput(expected);
        tc.setHidden(hidden);
        tc.setWeight(1);
        return tc;
    }

    private void signIn(UUID userId, String role) {
        LmsUserDetails user = new LmsUserDetails(userId, "user@srp.edu.in", "User", null, true, false,
                Set.of(role), Set.of(), null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }
}
