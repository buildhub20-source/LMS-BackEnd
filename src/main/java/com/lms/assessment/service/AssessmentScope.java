package com.lms.assessment.service;

import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.SectionRepository;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.security.authentication.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/** Ownership checks for nested routes that do not carry an assessment ID. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssessmentScope {
    private final SectionRepository sections;
    private final AssessmentQuestionRepository questions;

    public void requireSection(UUID id) {
        var section = sections.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Section", id));
        requireOwner(section.getAssessment().getCreatedBy());
    }

    public void requireAssessmentQuestion(UUID id) {
        var question = questions.findById(id).orElseThrow(() -> ResourceNotFoundException.of("AssessmentQuestion", id));
        requireOwner(question.getAssessment().getCreatedBy());
    }

    public void requireQuestion(UUID id) {
        var principal = AuthenticationService.requirePrincipal();
        if (!principal.getRoles().contains("INSTRUCTOR") || GradingAccess.isAdministrator(principal)) return;
        var links = questions.findByQuestionId(id);
        if (links.isEmpty()) throw new AccessDeniedException("This question has no assessment you own");
        // An edit changes every linked assessment, including shared questions.
        links.forEach(link -> requireOwner(link.getAssessment().getCreatedBy()));
    }

    private void requireOwner(UUID owner) {
        var principal = AuthenticationService.requirePrincipal();
        if (principal.getRoles().contains("INSTRUCTOR") && !GradingAccess.isAdministrator(principal)
                && !principal.getUserId().equals(owner)) {
            throw new AccessDeniedException("You may only manage your own assessments");
        }
    }
}
