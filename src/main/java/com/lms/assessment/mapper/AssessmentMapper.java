package com.lms.assessment.mapper;

import com.lms.assessment.dto.response.AssessmentResponse;
import com.lms.assessment.dto.response.AssessmentSummaryResponse;
import com.lms.assessment.entity.Assessment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for Assessment entities.
 *
 * <p>{@code questionCount} cannot be derived from the entity alone (it requires
 * a repository count), so it is passed as a separate parameter via
 * {@code @Mapping(source = "questionCount", ...)}.
 */
@Mapper(componentModel = "spring")
public interface AssessmentMapper {

    @Mapping(source = "assessment.id",              target = "id")
    @Mapping(source = "assessment.title",           target = "title")
    @Mapping(source = "assessment.description",     target = "description")
    @Mapping(source = "assessment.durationMinutes", target = "durationMinutes")
    @Mapping(source = "assessment.totalMarks",      target = "totalMarks")
    @Mapping(source = "assessment.maxAttempts",     target = "maxAttempts")
    @Mapping(source = "assessment.randomizeQuestions", target = "randomizeQuestions")
    @Mapping(source = "assessment.retakePolicy",    target = "retakePolicy")
    @Mapping(source = "assessment.status",          target = "status")
    @Mapping(source = "assessment.startTime",       target = "startTime")
    @Mapping(source = "assessment.endTime",         target = "endTime")
    @Mapping(source = "assessment.createdBy",       target = "createdBy")
    @Mapping(source = "assessment.createdAt",       target = "createdAt")
    @Mapping(source = "assessment.updatedAt",       target = "updatedAt")
    @Mapping(source = "questionCount",              target = "questionCount")
    AssessmentResponse toResponse(Assessment assessment, long questionCount);

    @Mapping(source = "assessment.id",              target = "id")
    @Mapping(source = "assessment.title",           target = "title")
    @Mapping(source = "assessment.durationMinutes", target = "durationMinutes")
    @Mapping(source = "assessment.totalMarks",      target = "totalMarks")
    @Mapping(source = "assessment.maxAttempts",     target = "maxAttempts")
    @Mapping(source = "assessment.status",          target = "status")
    @Mapping(source = "assessment.startTime",       target = "startTime")
    @Mapping(source = "assessment.endTime",         target = "endTime")
    @Mapping(source = "assessment.createdAt",       target = "createdAt")
    @Mapping(source = "questionCount",              target = "questionCount")
    AssessmentSummaryResponse toSummaryResponse(Assessment assessment, long questionCount);
}
