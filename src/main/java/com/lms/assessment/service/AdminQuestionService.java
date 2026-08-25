package com.lms.assessment.service;

import com.lms.assessment.dto.request.CreateQuestionRequest;
import com.lms.assessment.dto.request.UpdateQuestionRequest;
import com.lms.assessment.dto.response.QuestionResponse;

import java.util.List;
import java.util.UUID;

public interface AdminQuestionService {

    /** Add a new question to an assessment (unsectioned). */
    QuestionResponse addQuestion(UUID assessmentId, CreateQuestionRequest request);

    /** Add a new question to a specific section within an assessment. */
    QuestionResponse addQuestion(UUID assessmentId, UUID sectionId, CreateQuestionRequest request);

    /** Get all questions for an assessment in order. */
    List<QuestionResponse> getQuestionsByAssessmentId(UUID assessmentId);

    /** Update an existing question and its test cases. */
    QuestionResponse updateQuestion(UUID questionId, UpdateQuestionRequest request);

    /** Remove a question from an assessment. */
    void removeQuestionFromAssessment(UUID assessmentId, UUID questionId);
}
