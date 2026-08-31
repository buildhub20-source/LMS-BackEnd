package com.lms.assessment.dto.response;

public record GradeDistributionDto(
        String gradeLetter,
        String label,
        long count,
        double percentage
) {}
