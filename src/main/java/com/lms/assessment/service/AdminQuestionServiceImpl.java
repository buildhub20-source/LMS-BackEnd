package com.lms.assessment.service;

import com.lms.assessment.dto.request.CreateQuestionOptionRequest;
import com.lms.assessment.dto.request.CreateQuestionRequest;
import com.lms.assessment.dto.request.CreateTestCaseRequest;
import com.lms.assessment.dto.request.UpdateQuestionRequest;
import com.lms.assessment.dto.response.QuestionOptionResponse;
import com.lms.assessment.dto.response.QuestionResponse;
import com.lms.assessment.dto.response.TestCaseResponse;
import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentQuestion;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.QuestionOption;
import com.lms.assessment.entity.QuestionType;
import com.lms.assessment.entity.TestCase;
import com.lms.assessment.mapper.QuestionMapper;
import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.AssessmentRepository;
import com.lms.assessment.repository.QuestionOptionRepository;
import com.lms.assessment.repository.QuestionRepository;
import com.lms.assessment.repository.TestCaseRepository;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQuestionServiceImpl implements AdminQuestionService {

    private final AssessmentRepository assessmentRepository;
    private final QuestionRepository questionRepository;
    private final TestCaseRepository testCaseRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final QuestionMapper questionMapper;

    @Override
    @Transactional
    public QuestionResponse addQuestion(UUID assessmentId, CreateQuestionRequest request) {
        Assessment assessment = requireAssessment(assessmentId);
        requireDraft(assessment, "add question to");

        CreateQuestionRequest req = request.withDefaults();

        // 1. Create and save Question
        Question question = Question.builder()
                .title(req.title().trim())
                .description(req.description())
                .inputFormat(req.inputFormat())
                .outputFormat(req.outputFormat())
                .constraints(req.constraints())
                .difficulty(req.difficulty())
                .questionType(req.questionType())
                .marks(req.marks())
                .timeLimitMs(req.timeLimitMs())
                .memoryLimitMb(req.memoryLimitMb())
                .build();

        Question savedQuestion = questionRepository.save(question);

        // 2. Save Test Cases or Options based on question type
        List<TestCase> testCases = List.of();
        List<QuestionOption> options = List.of();

        if (req.questionType() == QuestionType.MULTIPLE_CHOICE) {
            if (req.options() == null || req.options().size() < 2) {
                throw new BusinessRuleException("Multiple choice questions must have at least 2 options");
            }
            boolean hasCorrect = req.options().stream().anyMatch(CreateQuestionOptionRequest::isCorrect);
            if (!hasCorrect) {
                throw new BusinessRuleException("Multiple choice questions must have at least 1 correct option marked");
            }
            options = saveQuestionOptions(savedQuestion, req.options());
        } else {
            testCases = saveTestCases(savedQuestion, req.testCases());
        }

        // 3. Create AssessmentQuestion junction
        int nextOrder = (int) assessmentQuestionRepository.countByAssessmentId(assessmentId);
        AssessmentQuestion assessmentQuestion = AssessmentQuestion.builder()
                .assessment(assessment)
                .question(savedQuestion)
                .questionOrder(nextOrder)
                .marks(req.marks())
                .build();

        assessmentQuestionRepository.save(assessmentQuestion);

        // 4. Update Assessment totalMarks
        recalculateTotalMarks(assessment);

        log.info("Admin added question {} to assessment {}", savedQuestion.getId(), assessmentId);

        List<TestCaseResponse> tcResponses = questionMapper.toTestCaseResponseList(testCases);
        List<QuestionOptionResponse> optResponses = questionMapper.toQuestionOptionResponseList(options);
        return questionMapper.toQuestionResponse(savedQuestion, nextOrder, req.marks(), tcResponses, optResponses);
    }

    @Override
    public List<QuestionResponse> getQuestionsByAssessmentId(UUID assessmentId) {
        requireAssessment(assessmentId);

        List<AssessmentQuestion> junctions =
                assessmentQuestionRepository.findByAssessmentIdOrderByQuestionOrderAsc(assessmentId);

        List<QuestionResponse> result = new ArrayList<>();
        for (AssessmentQuestion aq : junctions) {
            Question q = aq.getQuestion();
            List<TestCase> tcs = testCaseRepository.findByQuestionIdOrderByIdAsc(q.getId());
            List<TestCaseResponse> tcResponses = questionMapper.toTestCaseResponseList(tcs);
            List<QuestionOption> opts = questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(q.getId());
            List<QuestionOptionResponse> optResponses = questionMapper.toQuestionOptionResponseList(opts);
            UUID sectionId = aq.getSection() != null ? aq.getSection().getId() : null;
            result.add(questionMapper.toQuestionResponse(q, aq.getQuestionOrder(), aq.getMarks(), sectionId, tcResponses, optResponses));
        }

        return result;
    }

    @Override
    @Transactional
    public QuestionResponse updateQuestion(UUID questionId, UpdateQuestionRequest request) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Question", questionId));

        if (StringUtils.hasText(request.title())) {
            question.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            question.setDescription(request.description());
        }
        if (request.inputFormat() != null) {
            question.setInputFormat(request.inputFormat());
        }
        if (request.outputFormat() != null) {
            question.setOutputFormat(request.outputFormat());
        }
        if (request.constraints() != null) {
            question.setConstraints(request.constraints());
        }
        if (request.difficulty() != null) {
            question.setDifficulty(request.difficulty());
        }
        if (request.marks() != null) {
            question.setMarks(request.marks());
        }
        if (request.timeLimitMs() != null) {
            question.setTimeLimitMs(request.timeLimitMs());
        }
        if (request.memoryLimitMb() != null) {
            question.setMemoryLimitMb(request.memoryLimitMb());
        }

        Question saved = questionRepository.save(question);

        // Sync marks on every AssessmentQuestion junction that references this question,
        // then recalculate each affected assessment's totalMarks.
        if (request.marks() != null) {
            List<AssessmentQuestion> junctions =
                    assessmentQuestionRepository.findByQuestionId(questionId);
            for (AssessmentQuestion aq : junctions) {
                aq.setMarks(request.marks());
                assessmentQuestionRepository.save(aq);
                recalculateTotalMarks(aq.getAssessment());
            }
        }

        // Update test cases if provided (for coding questions)
        List<TestCase> testCases;
        if (request.testCases() != null) {
            List<TestCase> existing = testCaseRepository.findByQuestionIdOrderByIdAsc(questionId);
            testCaseRepository.deleteAll(existing);
            testCases = saveTestCases(saved, request.testCases());
        } else {
            testCases = testCaseRepository.findByQuestionIdOrderByIdAsc(questionId);
        }

        // Update options if provided (for MCQ questions)
        List<QuestionOption> options;
        if (request.options() != null) {
            if (saved.getQuestionType() == QuestionType.MULTIPLE_CHOICE) {
                if (request.options().size() < 2) {
                    throw new BusinessRuleException("Multiple choice questions must have at least 2 options");
                }
                boolean hasCorrect = request.options().stream().anyMatch(CreateQuestionOptionRequest::isCorrect);
                if (!hasCorrect) {
                    throw new BusinessRuleException("Multiple choice questions must have at least 1 correct option marked");
                }
            }
            List<QuestionOption> existingOpts = questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(questionId);
            questionOptionRepository.deleteAll(existingOpts);
            options = saveQuestionOptions(saved, request.options());
        } else {
            options = questionOptionRepository.findByQuestionIdOrderByOrderIndexAsc(questionId);
        }

        // Resolve the question order from the first junction found (may be in multiple assessments)
        List<AssessmentQuestion> allJunctions = assessmentQuestionRepository.findByQuestionId(questionId);
        int resolvedOrder = allJunctions.isEmpty() ? 0 : allJunctions.get(0).getQuestionOrder();
        UUID resolvedSectionId = (allJunctions.isEmpty() || allJunctions.get(0).getSection() == null)
                ? null : allJunctions.get(0).getSection().getId();
        int resolvedMarks = saved.getMarks();

        List<TestCaseResponse> tcResponses = questionMapper.toTestCaseResponseList(testCases);
        List<QuestionOptionResponse> optResponses = questionMapper.toQuestionOptionResponseList(options);
        return questionMapper.toQuestionResponse(saved, resolvedOrder, resolvedMarks, resolvedSectionId, tcResponses, optResponses);
    }

    @Override
    @Transactional
    public void removeQuestionFromAssessment(UUID assessmentId, UUID questionId) {
        Assessment assessment = requireAssessment(assessmentId);
        requireDraft(assessment, "remove question from");

        List<AssessmentQuestion> junctions =
                assessmentQuestionRepository.findByAssessmentIdOrderByQuestionOrderAsc(assessmentId);

        AssessmentQuestion target = junctions.stream()
                .filter(aq -> aq.getQuestion().getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Question in assessment", questionId));

        assessmentQuestionRepository.delete(target);

        // Recalculate order for remaining questions
        int order = 0;
        for (AssessmentQuestion aq : junctions) {
            if (!aq.getQuestion().getId().equals(questionId)) {
                aq.setQuestionOrder(order++);
                assessmentQuestionRepository.save(aq);
            }
        }

        recalculateTotalMarks(assessment);
        log.info("Admin removed question {} from assessment {}", questionId, assessmentId);
    }

    // ---------------------------------------------------------------
    // Utilities
    // ---------------------------------------------------------------

    private List<TestCase> saveTestCases(Question question, List<CreateTestCaseRequest> requests) {
        List<TestCase> list = new ArrayList<>();
        if (requests == null) return list;

        for (CreateTestCaseRequest req : requests) {
            CreateTestCaseRequest tcReq = req.withDefaults();
            TestCase tc = TestCase.builder()
                    .question(question)
                    .inputData(tcReq.inputData())
                    .expectedOutput(tcReq.expectedOutput())
                    .sample(tcReq.sample())
                    .hidden(tcReq.hidden())
                    .weight(tcReq.weight())
                    .build();
            list.add(testCaseRepository.save(tc));
        }
        return list;
    }

    private List<QuestionOption> saveQuestionOptions(Question question, List<CreateQuestionOptionRequest> requests) {
        List<QuestionOption> list = new ArrayList<>();
        if (requests == null) return list;

        int idx = 0;
        for (CreateQuestionOptionRequest optReq : requests) {
            QuestionOption opt = QuestionOption.builder()
                    .question(question)
                    .optionText(optReq.optionText().trim())
                    .correct(optReq.isCorrect())
                    .orderIndex(optReq.orderIndex() != null ? optReq.orderIndex() : idx++)
                    .explanation(optReq.explanation())
                    .build();
            list.add(questionOptionRepository.save(opt));
        }
        return list;
    }

    private void recalculateTotalMarks(Assessment assessment) {
        List<AssessmentQuestion> questions =
                assessmentQuestionRepository.findByAssessmentIdOrderByQuestionOrderAsc(assessment.getId());

        int total = questions.stream().mapToInt(AssessmentQuestion::getMarks).sum();
        assessment.setTotalMarks(total);
        assessmentRepository.save(assessment);
    }

    private Assessment requireAssessment(UUID id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Assessment", id));
    }

    private static void requireDraft(Assessment assessment, String action) {
        if (!assessment.isDraft()) {
            throw new BusinessRuleException(
                    "Cannot " + action + " assessment: status is " + assessment.getStatus()
                    + ". Only DRAFT assessments may be modified.");
        }
    }
}
