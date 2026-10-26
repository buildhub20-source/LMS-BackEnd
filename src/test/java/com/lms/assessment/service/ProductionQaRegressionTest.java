package com.lms.assessment.service;

import com.lms.assessment.controller.SubmissionHistoryController;
import com.lms.assessment.entity.*;
import com.lms.assessment.repository.*;
import com.lms.common.util.PublishedContent;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import java.time.Instant;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class ProductionQaRegressionTest {
    private final UUID instructor = UUID.randomUUID();
    private final SectionRepository sections = mock(SectionRepository.class);
    private final AssessmentQuestionRepository links = mock(AssessmentQuestionRepository.class);
    private final AssessmentScope scope = new AssessmentScope(sections, links);

    private void signIn(String... roles) {
        var user = new LmsUserDetails(instructor, "qa@example.test", "QA", null,
                true, false, Set.of(roles), Set.of(), null);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @AfterEach void clearAuthentication() { SecurityContextHolder.clearContext(); }

    @Test void instructorCannotModifyAnotherInstructorsSection() {
        signIn("INSTRUCTOR");
        var id = UUID.randomUUID();
        var section = Section.builder().assessment(Assessment.builder().createdBy(UUID.randomUUID()).build()).build();
        when(sections.findById(id)).thenReturn(Optional.of(section));
        assertThatThrownBy(() -> scope.requireSection(id)).isInstanceOf(AccessDeniedException.class);
        section.getAssessment().setCreatedBy(instructor);
        assertThatCode(() -> scope.requireSection(id)).doesNotThrowAnyException();
    }

    @Test void sharedQuestionsCannotBeChangedAcrossInstructorOwnership() {
        signIn("INSTRUCTOR");
        var questionId = UUID.randomUUID();
        var mine = new AssessmentQuestion();
        mine.setAssessment(Assessment.builder().createdBy(instructor).build());
        var other = new AssessmentQuestion();
        other.setAssessment(Assessment.builder().createdBy(UUID.randomUUID()).build());
        when(links.findByQuestionId(questionId)).thenReturn(List.of(mine, other));
        assertThatThrownBy(() -> scope.requireQuestion(questionId)).isInstanceOf(AccessDeniedException.class);
        when(links.findByQuestionId(questionId)).thenReturn(List.of(mine));
        assertThatCode(() -> scope.requireQuestion(questionId)).doesNotThrowAnyException();
    }

    @Test void administratorWithInstructorRoleRetainsAdministrativeAccess() {
        signIn("ADMIN", "INSTRUCTOR");
        assertThatCode(() -> scope.requireQuestion(UUID.randomUUID())).doesNotThrowAnyException();
        verifyNoInteractions(links);
    }

    @Test void historyScopeIsDerivedFromTheAuthenticatedPrincipal() {
        var service = mock(SubmissionHistoryService.class);
        var controller = new SubmissionHistoryController(service);
        signIn("INSTRUCTOR");
        controller.list();
        verify(service).list(instructor);
        signIn("ADMIN", "INSTRUCTOR");
        controller.list();
        verify(service).list(null);
    }

    @Test void historyUsesBulkQueriesAndDoesNotGradeUnscoredAttemptsAsFailures() {
        var attempts = mock(AssessmentAttemptRepository.class);
        var users = mock(UserRepository.class);
        var service = new SubmissionHistoryService(attempts, users);
        var student = UUID.randomUUID();
        var assessment = Assessment.builder().id(UUID.randomUUID()).createdBy(instructor)
                .title("Java Quiz").totalMarks(100).durationMinutes(30).status(AssessmentStatus.PUBLISHED).build();
        var latest = AssessmentAttempt.builder().assessment(assessment).studentId(student)
                .startedAt(Instant.now()).status(AttemptStatus.SUBMITTED).submittedAt(Instant.now()).build();
        var earlier = AssessmentAttempt.builder().assessment(assessment).studentId(student)
                .startedAt(Instant.now().minusSeconds(3600)).score(90).status(AttemptStatus.SUBMITTED).build();
        var user = new User(); user.setId(student); user.setName("Learner"); user.setEmail("learner@example.test");
        when(attempts.findByAssessment_CreatedByOrderByStartedAtDesc(instructor)).thenReturn(List.of(latest, earlier));
        when(users.findAllById(any())).thenReturn(List.of(user));
        var rows = service.list(instructor);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).attemptsCount()).isEqualTo(2);
        assertThat(rows.get(0).studentName()).isEqualTo("Learner");
        assertThat(rows.get(0).status()).isEqualTo("COMPLETED");
        assertThat(rows.get(0).score()).isNull();
        assertThat(rows.get(0).passed()).isNull();
        assertThat(rows.get(0).gradeLetter()).isNull();
        verify(attempts, never()).findAllByOrderByStartedAtDesc();
        verify(users, times(1)).findAllById(any());
        verifyNoMoreInteractions(users);
    }

    @Test void emptyHistoryNeedsNoUserLookup() {
        var attempts = mock(AssessmentAttemptRepository.class);
        var users = mock(UserRepository.class);
        assertThat(new SubmissionHistoryService(attempts, users).list(null)).isEmpty();
        verifyNoInteractions(users);
    }

    @Test void knownSeedPromptsAreSuppressedWithoutChangingRealCourseText() {
        assertThat(PublishedContent.description("React", "Do you want to support both video lessons and quizzes?")).isNull();
        assertThat(PublishedContent.description("Angular", "Mastering Illustrator course")).isNull();
        assertThat(PublishedContent.description("Illustrator", "Mastering Illustrator course")).isEqualTo("Mastering Illustrator course");
        assertThat(PublishedContent.description("Java", "Learn Java through practical projects.")).isEqualTo("Learn Java through practical projects.");
    }
}
