package com.lms.assessment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.assessment.ai.AiGradingClient;
import com.lms.assessment.ai.AiGradingException;
import com.lms.assessment.dto.response.AiGradingSuggestionResponse;
import com.lms.assessment.entity.AiGradingResult;
import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.QuestionType;
import com.lms.assessment.entity.Submission;
import com.lms.assessment.repository.AiGradingResultRepository;
import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.QuestionRepository;
import com.lms.assessment.repository.RubricCriterionRepository;
import com.lms.assessment.repository.SubmissionRepository;
import com.lms.assessment.repository.TestCaseRepository;
import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.security.authentication.AuthenticationService;
import com.lms.security.authentication.LmsUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

/**
 * On-demand AI grading suggestions for coding answers. The suggestion is only
 * advice: scores reach the student only when an instructor saves a grade.
 */
@Slf4j
@Service
public class AiGradingService {

    private final SubmissionRepository submissionRepository;
    private final QuestionRepository questionRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final TestCaseRepository testCaseRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final AiGradingResultRepository resultRepository;
    private final AiGradingClient client;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate readOnly;

    public AiGradingService(SubmissionRepository submissionRepository, QuestionRepository questionRepository,
                            AssessmentQuestionRepository assessmentQuestionRepository,
                            TestCaseRepository testCaseRepository,
                            RubricCriterionRepository rubricCriterionRepository,
                            AiGradingResultRepository resultRepository, AiGradingClient client,
                            ObjectMapper objectMapper, PlatformTransactionManager transactionManager) {
        this.submissionRepository = submissionRepository;
        this.questionRepository = questionRepository;
        this.assessmentQuestionRepository = assessmentQuestionRepository;
        this.testCaseRepository = testCaseRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.resultRepository = resultRepository;
        this.client = client;
        this.objectMapper = objectMapper;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    /**
     * Asks the AI service to grade one answer. The call can take minutes, so it
     * runs outside any transaction: reads first, then the call, then one save.
     */
    public AiGradingSuggestionResponse suggest(UUID submissionId, UUID rubricId) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        AiGradingClient.GradeRequest request = readOnly.execute(status -> buildRequest(submissionId, rubricId, principal));

        if (!client.isEnabled()) {
            throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, "AI grading is not enabled for this workspace");
        }

        AiGradingResult result;
        try {
            JsonNode data = client.evaluate(request);
            result = suggested(submissionId, principal.getUserId(), data);
        } catch (AiGradingException ex) {
            resultRepository.save(failed(submissionId, principal.getUserId(), ex));
            if (ex.isRetryable()) {
                throw new ApplicationException(ErrorCode.SERVICE_UNAVAILABLE, ex.getMessage());
            }
            throw new BusinessRuleException(ex.getMessage());
        }

        AiGradingResult saved = resultRepository.save(result);
        log.info("AI grading suggestion {} for submission {}: {}/{} ({} in, {} out tokens)", saved.getId(),
                submissionId, saved.getScore(), saved.getMaxScore(), saved.getInputTokens(), saved.getOutputTokens());
        return toResponse(saved);
    }

    /** The most recent suggestion for an answer, if one was requested. */
    public AiGradingSuggestionResponse latest(UUID submissionId) {
        LmsUserDetails principal = AuthenticationService.requirePrincipal();
        return readOnly.execute(status -> {
            Submission submission = requireSubmission(submissionId);
            GradingAccess.requireCanGrade(submission.getAttempt().getAssessment(), principal);
            return resultRepository.findFirstBySubmissionIdOrderByCreatedAtDesc(submissionId)
                    .map(this::toResponse)
                    .orElseThrow(() -> new ResourceNotFoundException("No AI suggestion has been requested for this answer"));
        });
    }

    private AiGradingClient.GradeRequest buildRequest(UUID submissionId, UUID rubricId, LmsUserDetails principal) {
        Submission submission = requireSubmission(submissionId);
        Assessment assessment = submission.getAttempt().getAssessment();
        GradingAccess.requireCanGrade(assessment, principal);

        Question question = questionRepository.findById(submission.getQuestionId())
                .orElseThrow(() -> ResourceNotFoundException.of("Question", submission.getQuestionId()));
        if (question.getQuestionType() != QuestionType.CODING) {
            throw new BusinessRuleException("AI grading is only available for coding questions");
        }
        if (!StringUtils.hasText(submission.getSourceCode())) {
            throw new BusinessRuleException("The student did not submit any code for this question");
        }

        List<AiGradingClient.TestCase> testCases = testCaseRepository.findByQuestionIdOrderByIdAsc(question.getId())
                .stream()
                .map(tc -> new AiGradingClient.TestCase(tc.getId().toString(), nullToEmpty(tc.getInputData()),
                        tc.getExpectedOutput(), tc.isHidden(), Math.max(1, tc.getWeight())))
                .toList();
        if (testCases.isEmpty()) {
            throw new BusinessRuleException("Add at least one test case to this question before using AI grading");
        }

        List<AiGradingClient.Criterion> rubric = rubricId == null ? List.of()
                : rubricCriterionRepository.findByRubricIdOrderByIdAsc(rubricId).stream()
                        .map(c -> new AiGradingClient.Criterion(c.getId().toString(), c.getCriterionName(),
                                Math.max(1, c.getMaxPoints())))
                        .toList();

        int marks = assessmentQuestionRepository.findByAssessmentIdAndQuestionId(assessment.getId(), question.getId())
                .map(aq -> aq.getMarks())
                .filter(m -> m > 0)
                .orElse(Math.max(1, question.getMarks()));

        return new AiGradingClient.GradeRequest(
                new AiGradingClient.Question(question.getTitle(), nullToEmpty(question.getDescription()),
                        nullToEmpty(question.getInputFormat()), nullToEmpty(question.getOutputFormat()),
                        nullToEmpty(question.getConstraints()), marks),
                testCases,
                rubric,
                new AiGradingClient.Submission(submission.getLanguage(), submission.getSourceCode()));
    }

    private Submission requireSubmission(UUID submissionId) {
        return submissionRepository.findById(submissionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Submission", submissionId));
    }

    private AiGradingResult suggested(UUID submissionId, UUID requestedBy, JsonNode data) {
        JsonNode usage = data.path("usage");
        return AiGradingResult.builder()
                .submissionId(submissionId)
                .requestedBy(requestedBy)
                .status(AiGradingResult.SUGGESTED)
                .score(data.hasNonNull("score") ? data.get("score").decimalValue() : null)
                .maxScore(data.hasNonNull("maxScore") ? data.get("maxScore").asInt() : null)
                .testsPassed(data.hasNonNull("testsPassed") ? data.get("testsPassed").asInt() : null)
                .testsTotal(data.hasNonNull("testsTotal") ? data.get("testsTotal").asInt() : null)
                .resultJson(data.toString())
                .inputTokens(usage.hasNonNull("inputTokens") ? usage.get("inputTokens").asInt() : null)
                .outputTokens(usage.hasNonNull("outputTokens") ? usage.get("outputTokens").asInt() : null)
                .build();
    }

    private AiGradingResult failed(UUID submissionId, UUID requestedBy, AiGradingException ex) {
        String message = ex.getMessage() == null ? null : ex.getMessage().substring(0, Math.min(500, ex.getMessage().length()));
        return AiGradingResult.builder()
                .submissionId(submissionId)
                .requestedBy(requestedBy)
                .status(AiGradingResult.FAILED)
                .errorCode(ex.getCode())
                .errorMessage(message)
                .build();
    }

    private AiGradingSuggestionResponse toResponse(AiGradingResult r) {
        JsonNode result = null;
        if (r.getResultJson() != null) {
            try {
                result = objectMapper.readTree(r.getResultJson());
                if (result.isObject()) {
                    ((com.fasterxml.jackson.databind.node.ObjectNode) result).remove("usage");
                }
            } catch (Exception ex) {
                log.warn("Stored AI result {} is not valid JSON", r.getId());
            }
        }
        return new AiGradingSuggestionResponse(r.getId(), r.getSubmissionId(), r.getStatus(), r.getScore(),
                r.getMaxScore(), r.getTestsPassed(), r.getTestsTotal(), result, r.getErrorCode(),
                r.getErrorMessage(), r.getCreatedAt());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
