package com.lms.assessment.controller;

import com.lms.assessment.dto.request.CreateQuestionRequest;
import com.lms.assessment.dto.request.UpdateQuestionRequest;
import com.lms.assessment.dto.response.QuestionResponse;
import com.lms.assessment.service.AdminQuestionService;
import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin — Questions")
@RestController
@RequestMapping(ApiPaths.ADMIN_ASSESSMENTS)
@RequiredArgsConstructor
public class AdminQuestionController {

    private final AdminQuestionService questionService;

    @Operation(summary = "Add a coding question with test cases to an assessment")
    @PostMapping("/{assessmentId}/questions")
    @PreAuthorize("hasAuthority('ASSESSMENT_CREATE')")
    public ResponseEntity<ApiResponse<QuestionResponse>> addQuestion(
            @PathVariable UUID assessmentId,
            @Valid @RequestBody CreateQuestionRequest request) {

        QuestionResponse response = questionService.addQuestion(assessmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(response, "Question added to assessment successfully"));
    }

    @Operation(summary = "Get all questions for an assessment")
    @GetMapping("/{assessmentId}/questions")
    @PreAuthorize("hasAuthority('ASSESSMENT_VIEW')")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> getQuestions(
            @PathVariable UUID assessmentId) {

        return ResponseEntity.ok(ApiResponse.of(questionService.getQuestionsByAssessmentId(assessmentId)));
    }

    @Operation(summary = "Update an existing question and its test cases")
    @PutMapping("/questions/{questionId}")
    @PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
    public ResponseEntity<ApiResponse<QuestionResponse>> updateQuestion(
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.of(questionService.updateQuestion(questionId, request), "Question updated"));
    }

    @Operation(summary = "Remove a question from an assessment")
    @DeleteMapping("/{assessmentId}/questions/{questionId}")
    @PreAuthorize("hasAuthority('ASSESSMENT_UPDATE')")
    public ResponseEntity<ApiResponse<Void>> removeQuestion(
            @PathVariable UUID assessmentId,
            @PathVariable UUID questionId) {

        questionService.removeQuestionFromAssessment(assessmentId, questionId);
        return ResponseEntity.ok(ApiResponse.message("Question removed from assessment"));
    }
}
