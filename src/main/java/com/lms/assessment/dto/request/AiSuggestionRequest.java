package com.lms.assessment.dto.request;

import java.util.UUID;

/** Optional rubric to grade against; without one the AI uses a default four-part rubric. */
public record AiSuggestionRequest(UUID rubricId) {}
