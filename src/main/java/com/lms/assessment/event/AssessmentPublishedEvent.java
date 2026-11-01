package com.lms.assessment.event;

import java.util.UUID;

/**
 * Raised when an assessment moves to PUBLISHED.
 *
 * <p>Assessments are not tied to a course, so every active student can see one
 * once it is published — which makes the whole student body the audience.
 */
public record AssessmentPublishedEvent(UUID assessmentId) {
}
