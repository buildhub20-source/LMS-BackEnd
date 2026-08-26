package com.lms.assessment.mapper;

import com.lms.assessment.dto.response.QuestionResponse;
import com.lms.assessment.dto.response.TestCaseResponse;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.TestCase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper
public interface QuestionMapper {

    TestCaseResponse toTestCaseResponse(TestCase testCase);

    List<TestCaseResponse> toTestCaseResponseList(List<TestCase> testCases);

    @Mapping(source = "question.id",            target = "id")
    @Mapping(source = "question.title",         target = "title")
    @Mapping(source = "question.description",   target = "description")
    @Mapping(source = "question.inputFormat",   target = "inputFormat")
    @Mapping(source = "question.outputFormat",  target = "outputFormat")
    @Mapping(source = "question.constraints",   target = "constraints")
    @Mapping(source = "question.difficulty",    target = "difficulty")
    @Mapping(source = "question.questionType",  target = "questionType")
    @Mapping(source = "marks",                  target = "marks")
    @Mapping(source = "question.timeLimitMs",   target = "timeLimitMs")
    @Mapping(source = "question.memoryLimitMb", target = "memoryLimitMb")
    @Mapping(source = "questionOrder",          target = "questionOrder")
    @Mapping(source = "question.createdAt",     target = "createdAt")
    @Mapping(source = "question.updatedAt",     target = "updatedAt")
    @Mapping(source = "testCases",              target = "testCases")
    QuestionResponse toQuestionResponse(Question question, int questionOrder, int marks, List<TestCaseResponse> testCases);
}
