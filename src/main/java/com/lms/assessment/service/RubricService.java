package com.lms.assessment.service;

import com.lms.assessment.dto.request.CreateRubricRequest;
import com.lms.assessment.dto.response.RubricResponse;
import com.lms.common.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RubricService {
    RubricResponse createRubric(CreateRubricRequest request, UUID createdBy);
    PageResponse<RubricResponse> listRubrics(Pageable pageable);
    RubricResponse getRubricById(UUID id);
    void deleteRubric(UUID id);
}
