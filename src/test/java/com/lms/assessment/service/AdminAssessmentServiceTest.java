package com.lms.assessment.service;

import com.lms.assessment.dto.request.CreateAssessmentRequest;
import com.lms.assessment.dto.request.UpdateAssessmentRequest;
import com.lms.assessment.dto.response.AssessmentResponse;
import com.lms.assessment.dto.response.AssessmentSummaryResponse;
import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentStatus;
import com.lms.assessment.mapper.AssessmentMapper;
import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.AssessmentRepository;
import com.lms.assessment.repository.TestCaseRepository;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAssessmentServiceImpl")
class AdminAssessmentServiceTest {

    @Mock private AssessmentRepository assessmentRepository;
    @Mock private AssessmentQuestionRepository assessmentQuestionRepository;
    @Mock private TestCaseRepository testCaseRepository;
    @Mock private AssessmentMapper assessmentMapper;

    @InjectMocks
    private AdminAssessmentServiceImpl service;

    private final UUID adminId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private Assessment draftAssessment() {
        return Assessment.builder()
                .id(assessmentId)
                .title("Java Fundamentals")
                .description("Basic Java assessment")
                .durationMinutes(60)
                .totalMarks(100)
                .maxAttempts(1)
                .status(AssessmentStatus.DRAFT)
                .createdBy(adminId)
                .build();
    }

    private AssessmentResponse stubResponse(Assessment assessment, long questionCount) {
        return new AssessmentResponse(
                assessment.getId(), assessment.getTitle(), assessment.getDescription(),
                assessment.getDurationMinutes(), assessment.getTotalMarks(),
                assessment.getMaxAttempts(), assessment.getStatus(),
                assessment.getStartTime(), assessment.getEndTime(),
                assessment.getCreatedBy(), Instant.now(), Instant.now(),
                questionCount
        );
    }

    // ---------------------------------------------------------------
    // Create
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("persists a DRAFT assessment and returns the response")
        void createsADraftAssessment() {
            CreateAssessmentRequest request = new CreateAssessmentRequest(
                    "Java Fundamentals", "Description", 60, 100, 1, null, null);

            Assessment saved = draftAssessment();
            when(assessmentRepository.save(any(Assessment.class))).thenReturn(saved);
            when(assessmentQuestionRepository.countByAssessmentId(saved.getId())).thenReturn(0L);

            AssessmentResponse expected = stubResponse(saved, 0);
            when(assessmentMapper.toResponse(saved, 0L)).thenReturn(expected);

            AssessmentResponse result = service.create(request, adminId);

            assertThat(result.status()).isEqualTo(AssessmentStatus.DRAFT);
            assertThat(result.title()).isEqualTo("Java Fundamentals");
            verify(assessmentRepository).save(any(Assessment.class));
        }

        @Test
        @DisplayName("applies default durationMinutes when not provided")
        void appliesDefaultDuration() {
            CreateAssessmentRequest request = new CreateAssessmentRequest(
                    "Test", null, null, null, null, null, null);

            when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> {
                Assessment a = invocation.getArgument(0);
                assertThat(a.getDurationMinutes()).isEqualTo(60);
                assertThat(a.getMaxAttempts()).isEqualTo(1);
                assertThat(a.getTotalMarks()).isEqualTo(0);
                return a;
            });
            when(assessmentQuestionRepository.countByAssessmentId(any())).thenReturn(0L);
            when(assessmentMapper.toResponse(any(), anyLong())).thenReturn(
                    new AssessmentResponse(assessmentId, "Test", null, 60, 0, 1,
                            AssessmentStatus.DRAFT, null, null, adminId, Instant.now(), Instant.now(), 0)
            );

            service.create(request, adminId);
        }
    }

    // ---------------------------------------------------------------
    // FindById
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("returns assessment when it exists")
        void returnsAssessment() {
            Assessment assessment = draftAssessment();
            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
            when(assessmentQuestionRepository.countByAssessmentId(assessmentId)).thenReturn(2L);
            when(assessmentMapper.toResponse(eq(assessment), eq(2L))).thenReturn(stubResponse(assessment, 2));

            AssessmentResponse result = service.findById(assessmentId);

            assertThat(result.id()).isEqualTo(assessmentId);
            assertThat(result.questionCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when assessment does not exist")
        void throwsWhenNotFound() {
            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(assessmentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ---------------------------------------------------------------
    // Update
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("patches only provided fields on a DRAFT assessment")
        void patchesTitle() {
            Assessment assessment = draftAssessment();
            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
            when(assessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(assessmentQuestionRepository.countByAssessmentId(any())).thenReturn(0L);
            when(assessmentMapper.toResponse(any(), anyLong())).thenReturn(stubResponse(assessment, 0));

            UpdateAssessmentRequest request = new UpdateAssessmentRequest(
                    "Updated Title", null, null, null, null, null);
            service.update(assessmentId, request);

            assertThat(assessment.getTitle()).isEqualTo("Updated Title");
            assertThat(assessment.getDurationMinutes()).isEqualTo(60); // unchanged
        }

        @Test
        @DisplayName("throws BusinessRuleException when assessment is not DRAFT")
        void refusesUpdateOnPublished() {
            Assessment assessment = draftAssessment();
            assessment.publish();
            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

            UpdateAssessmentRequest request = new UpdateAssessmentRequest(
                    "Title", null, null, null, null, null);

            assertThatThrownBy(() -> service.update(assessmentId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("PUBLISHED");
        }
    }

    // ---------------------------------------------------------------
    // Delete
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("deletes a DRAFT assessment")
        void deletesDraft() {
            Assessment assessment = draftAssessment();
            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

            service.delete(assessmentId);

            verify(assessmentRepository).delete(assessment);
        }

        @Test
        @DisplayName("refuses to delete a published assessment")
        void refusesDeleteOnPublished() {
            Assessment assessment = draftAssessment();
            assessment.publish();
            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

            assertThatThrownBy(() -> service.delete(assessmentId))
                    .isInstanceOf(BusinessRuleException.class);

            verify(assessmentRepository, never()).delete(any());
        }
    }

    // ---------------------------------------------------------------
    // Publish
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("publish()")
    class Publish {

        @Test
        @DisplayName("publishes a valid DRAFT assessment")
        void publishesValidDraft() {
            Assessment assessment = draftAssessment();
            UUID q1 = UUID.randomUUID();

            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
            when(assessmentQuestionRepository.countByAssessmentId(assessmentId)).thenReturn(1L);
            when(assessmentQuestionRepository.findQuestionIdsWithTestCases(assessmentId))
                    .thenReturn(List.of(q1));
            when(assessmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(assessmentMapper.toResponse(any(), anyLong()))
                    .thenReturn(stubResponse(assessment, 1));

            AssessmentResponse result = service.publish(assessmentId);

            assertThat(assessment.getStatus()).isEqualTo(AssessmentStatus.PUBLISHED);
        }

        @Test
        @DisplayName("rejects publish when no questions are attached")
        void rejectsPublishWithNoQuestions() {
            Assessment assessment = draftAssessment();
            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
            when(assessmentQuestionRepository.countByAssessmentId(assessmentId)).thenReturn(0L);

            assertThatThrownBy(() -> service.publish(assessmentId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("at least one question");
        }

        @Test
        @DisplayName("rejects publish when a question has no test cases")
        void rejectsPublishWhenQuestionHasNoTestCases() {
            Assessment assessment = draftAssessment();
            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));
            when(assessmentQuestionRepository.countByAssessmentId(assessmentId)).thenReturn(2L);
            // Only 1 question has test cases, but there are 2 questions → reject
            when(assessmentQuestionRepository.findQuestionIdsWithTestCases(assessmentId))
                    .thenReturn(List.of(UUID.randomUUID()));

            assertThatThrownBy(() -> service.publish(assessmentId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("test case");
        }

        @Test
        @DisplayName("rejects publish when totalMarks is zero")
        void rejectsPublishWithZeroMarks() {
            Assessment assessment = Assessment.builder()
                    .id(assessmentId)
                    .title("Test")
                    .durationMinutes(60)
                    .totalMarks(0)   // invalid
                    .maxAttempts(1)
                    .status(AssessmentStatus.DRAFT)
                    .createdBy(adminId)
                    .build();

            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

            assertThatThrownBy(() -> service.publish(assessmentId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("total marks");
        }

        @Test
        @DisplayName("rejects publish when assessment is already published")
        void rejectsPublishingAlreadyPublished() {
            Assessment assessment = draftAssessment();
            assessment.publish();
            when(assessmentRepository.findById(assessmentId)).thenReturn(Optional.of(assessment));

            assertThatThrownBy(() -> service.publish(assessmentId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("PUBLISHED");
        }
    }

    // ---------------------------------------------------------------
    // List
    // ---------------------------------------------------------------

    @Nested
    @DisplayName("list()")
    class ListTests {

        @Test
        @DisplayName("returns a paginated list of all assessments when no status filter")
        void listsAllAssessments() {
            Assessment assessment = draftAssessment();
            Page<Assessment> page = new PageImpl<>(List.of(assessment));
            Pageable pageable = PageRequest.of(0, 10);

            when(assessmentRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);
            when(assessmentQuestionRepository.countByAssessmentId(any())).thenReturn(0L);
            when(assessmentMapper.toSummaryResponse(any(), anyLong()))
                    .thenReturn(new AssessmentSummaryResponse(
                            assessmentId, "Java Fundamentals", 60, 100, 1,
                            AssessmentStatus.DRAFT, null, null, Instant.now(), 0));

            PageResponse<AssessmentSummaryResponse> result = service.list(null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }
}
