package com.lms.assessment.dto.response;

public record ScoreDistributionBucketDto(
        String rangeLabel, // e.g. "0-25%", "26-50%", "51-75%", "76-100%"
        long count
) {}
