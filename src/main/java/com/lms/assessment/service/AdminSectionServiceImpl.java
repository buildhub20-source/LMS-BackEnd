package com.lms.assessment.service;

import com.lms.assessment.dto.request.CreateSectionRequest;
import com.lms.assessment.dto.request.UpdateSectionRequest;
import com.lms.assessment.dto.response.QuestionResponse;
import com.lms.assessment.dto.response.SectionResponse;
import com.lms.assessment.dto.response.TestCaseResponse;
import com.lms.assessment.entity.Assessment;
import com.lms.assessment.entity.AssessmentQuestion;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.Section;
import com.lms.assessment.entity.TestCase;
import com.lms.assessment.mapper.QuestionMapper;
import com.lms.assessment.repository.AssessmentQuestionRepository;
import com.lms.assessment.repository.AssessmentRepository;
import com.lms.assessment.repository.SectionRepository;
import com.lms.assessment.repository.TestCaseRepository;
import com.lms.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSectionServiceImpl implements AdminSectionService {

    private final SectionRepository sectionRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final TestCaseRepository testCaseRepository;
    private final QuestionMapper questionMapper;

    @Override
    @Transactional
    public SectionResponse addSection(UUID assessmentId, CreateSectionRequest request) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assessment", assessmentId));

        int nextOrder = (int) sectionRepository.countByAssessmentId(assessmentId);

        Section section = Section.builder()
                .assessment(assessment)
                .title(request.title().trim())
                .description(request.description())
                .sectionOrder(nextOrder)
                .build();

        Section saved = sectionRepository.save(section);
        log.info("Created section '{}' in assessment {}", saved.getTitle(), assessmentId);

        return toResponse(saved);
    }

    @Override
    public List<SectionResponse> getSectionsByAssessmentId(UUID assessmentId) {
        assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Assessment", assessmentId));

        List<Section> sections = sectionRepository.findByAssessmentIdOrderBySectionOrderAsc(assessmentId);
        List<SectionResponse> result = new ArrayList<>();

        for (Section section : sections) {
            result.add(toResponse(section));
        }

        return result;
    }

    @Override
    @Transactional
    public SectionResponse updateSection(UUID sectionId, UpdateSectionRequest request) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Section", sectionId));

        if (StringUtils.hasText(request.title())) {
            section.setTitle(request.title().trim());
        }
        if (request.description() != null) {
            section.setDescription(request.description());
        }

        Section saved = sectionRepository.save(section);
        log.debug("Updated section {}", sectionId);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteSection(UUID sectionId) {
        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Section", sectionId));

        UUID assessmentId = section.getAssessment().getId();

        // Unlink questions from this section (set section_id to null) rather than deleting them
        List<AssessmentQuestion> linked = assessmentQuestionRepository
                .findBySectionIdOrderByQuestionOrderAsc(sectionId);
        for (AssessmentQuestion aq : linked) {
            aq.setSection(null);
            assessmentQuestionRepository.save(aq);
        }

        sectionRepository.delete(section);

        // Re-order remaining sections
        List<Section> remaining = sectionRepository
                .findByAssessmentIdOrderBySectionOrderAsc(assessmentId);
        int order = 0;
        for (Section s : remaining) {
            s.setSectionOrder(order++);
            sectionRepository.save(s);
        }

        log.info("Deleted section {} from assessment {}", sectionId, assessmentId);
    }

    @Override
    @Transactional
    public void moveQuestionToSection(UUID assessmentQuestionId, UUID sectionId) {
        AssessmentQuestion aq = assessmentQuestionRepository.findById(assessmentQuestionId)
                .orElseThrow(() -> ResourceNotFoundException.of("AssessmentQuestion", assessmentQuestionId));

        if (sectionId == null) {
            // Move to unsectioned
            aq.setSection(null);
        } else {
            Section section = sectionRepository.findById(sectionId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Section", sectionId));
            aq.setSection(section);
        }

        assessmentQuestionRepository.save(aq);
        log.debug("Moved question {} to section {}", assessmentQuestionId, sectionId);
    }

    // ---------------------------------------------------------------
    // Private utilities
    // ---------------------------------------------------------------

    private SectionResponse toResponse(Section section) {
        // Get all questions linked to this section
        List<AssessmentQuestion> junctions = assessmentQuestionRepository
                .findBySectionIdOrderByQuestionOrderAsc(section.getId());

        List<QuestionResponse> questionResponses = new ArrayList<>();
        for (AssessmentQuestion aq : junctions) {
            Question q = aq.getQuestion();
            List<TestCase> tcs = testCaseRepository.findByQuestionIdOrderByIdAsc(q.getId());
            List<TestCaseResponse> tcResponses = questionMapper.toTestCaseResponseList(tcs);
            questionResponses.add(questionMapper.toQuestionResponse(
                    q, aq.getQuestionOrder(), aq.getMarks(), section.getId(), tcResponses));
        }

        return new SectionResponse(
                section.getId(),
                section.getTitle(),
                section.getDescription(),
                section.getSectionOrder(),
                section.getCreatedAt(),
                section.getUpdatedAt(),
                questionResponses
        );
    }
}
