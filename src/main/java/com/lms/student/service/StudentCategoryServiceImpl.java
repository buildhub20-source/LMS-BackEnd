package com.lms.student.service;

import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceAlreadyExistsException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.student.dto.request.StudentCategoryRequest;
import com.lms.student.dto.response.StudentCategoryResponse;
import com.lms.student.entity.StudentCategory;
import com.lms.student.repository.StudentCategoryRepository;
import com.lms.student.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudentCategoryServiceImpl implements StudentCategoryService {

    private final StudentCategoryRepository categoryRepository;
    private final StudentProfileRepository studentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<StudentCategoryResponse> findAll() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public StudentCategoryResponse create(StudentCategoryRequest request) {
        String name = request.getName().trim();
        requireNameFree(name, null);

        StudentCategory saved = categoryRepository.save(StudentCategory.builder()
                .name(name)
                .description(trimToNull(request.getDescription()))
                .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                .build());

        log.info("Student category {} created", saved.getName());
        return toResponse(saved);
    }

    @Override
    public StudentCategoryResponse update(UUID id, StudentCategoryRequest request) {
        StudentCategory category = requireCategory(id);

        String name = request.getName().trim();
        requireNameFree(name, id);

        category.setName(name);
        category.setDescription(trimToNull(request.getDescription()));
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        return toResponse(categoryRepository.save(category));
    }

    @Override
    public void delete(UUID id) {
        StudentCategory category = requireCategory(id);

        // student_profiles.category_id is ON DELETE SET NULL, so the database
        // would silently blank the category on every learner using it. Refusing
        // here keeps that from happening by accident.
        long inUse = studentRepository.countByCategoryId(id);
        if (inUse > 0) {
            throw new BusinessRuleException("Category " + category.getName() + " is assigned to "
                    + inUse + " learner(s). Reassign them before deleting it.");
        }

        categoryRepository.delete(category);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    /** Names are unique; on update the row being edited is not its own clash. */
    private void requireNameFree(String name, UUID currentId) {
        categoryRepository.findByNameIgnoreCase(name)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw ResourceAlreadyExistsException.of("Student category", name);
                });
    }

    private StudentCategory requireCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Student category", id));
    }

    private StudentCategoryResponse toResponse(StudentCategory category) {
        StudentCategoryResponse resp = new StudentCategoryResponse();
        resp.setId(category.getId());
        resp.setName(category.getName());
        resp.setDescription(category.getDescription());
        resp.setSortOrder(category.getSortOrder());
        resp.setLearnerCount(studentRepository.countByCategoryId(category.getId()));
        return resp;
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
