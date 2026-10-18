package com.lms.assessment.service;

import com.lms.assessment.entity.Assessment;
import com.lms.security.authentication.LmsUserDetails;
import org.springframework.security.access.AccessDeniedException;

/**
 * Who may grade an assessment: tenant administrators may grade any; instructors
 * and delegated evaluators only the assessments they created.
 */
public final class GradingAccess {

    private GradingAccess() {}

    public static boolean isAdministrator(LmsUserDetails principal) {
        return principal.getRoles().contains("ADMIN") || principal.getRoles().contains("SUPER_ADMIN");
    }

    public static void requireCanGrade(Assessment assessment, LmsUserDetails principal) {
        if (!isAdministrator(principal) && !principal.getUserId().equals(assessment.getCreatedBy())) {
            throw new AccessDeniedException("You can only grade assessments you created");
        }
    }
}
