package com.lms.student.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Everything the intake form needs to populate its dropdowns, in one response —
 * several round trips to render one form is not worth it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentReferenceDataResponse {

    /** Only batches worth enrolling into: PLANNED and ONGOING. */
    private List<BatchResponse> batches;

    private List<ReferenceItemResponse> categories;
    private List<String> genders;
    private List<String> idProofTypes;
    private List<String> enrolmentStatuses;
}
