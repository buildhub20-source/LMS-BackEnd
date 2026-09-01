package com.lms.assessment.service;

import com.lms.assessment.dto.request.CreateRubricRequest;
import com.lms.assessment.dto.request.RubricCriterionDto;
import com.lms.assessment.dto.response.RubricResponse;
import com.lms.assessment.entity.Rubric;
import com.lms.assessment.entity.RubricCriterion;
import com.lms.assessment.repository.RubricRepository;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RubricServiceImpl implements RubricService {

    private final RubricRepository rubricRepository;

    @Override
    @Transactional
    public RubricResponse createRubric(CreateRubricRequest request, UUID createdBy) {
        Rubric rubric = Rubric.builder()
                .title(request.title().trim())
                .description(request.description())
                .createdBy(createdBy)
                .build();

        if (request.criteria() != null) {
            for (RubricCriterionDto dto : request.criteria()) {
                RubricCriterion criterion = RubricCriterion.builder()
                        .rubric(rubric)
                        .criterionName(dto.criterionName().trim())
                        .description(dto.description())
                        .maxPoints(dto.maxPoints())
                        .weight(dto.weight() > 0 ? dto.weight() : 1.0)
                        .build();
                rubric.addCriterion(criterion);
            }
        }

        Rubric saved = rubricRepository.save(rubric);
        log.info("Created rubric {} with {} criteria by user {}", saved.getId(), saved.getCriteria().size(), createdBy);
        return toResponse(saved);
    }

    @Override
    public PageResponse<RubricResponse> listRubrics(Pageable pageable) {
        Page<Rubric> page = rubricRepository.findByOrderByCreatedAtDesc(pageable);
        return PageResponse.from(page, this::toResponse);
    }

    @Override
    public RubricResponse getRubricById(UUID id) {
        Rubric rubric = rubricRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Rubric", id));
        return toResponse(rubric);
    }

    @Override
    @Transactional
    public void deleteRubric(UUID id) {
        Rubric rubric = rubricRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Rubric", id));
        rubricRepository.delete(rubric);
        log.info("Deleted rubric {}", id);
    }

    private RubricResponse toResponse(Rubric rubric) {
        var criteria = rubric.getCriteria().stream()
                .map(c -> new RubricResponse.RubricCriterionResponse(
                        c.getId(),
                        c.getCriterionName(),
                        c.getDescription(),
                        c.getMaxPoints(),
                        c.getWeight()
                ))
                .toList();

        return new RubricResponse(
                rubric.getId(),
                rubric.getTitle(),
                rubric.getDescription(),
                rubric.getCreatedBy(),
                rubric.getCreatedAt(),
                rubric.getUpdatedAt(),
                criteria
        );
    }
}
