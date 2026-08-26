package com.lms.instructor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Dropdown options for the instructor intake form, in one response. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorReferenceDataResponse {

    private List<String> genders;
    private List<String> idProofTypes;
    private List<String> employmentTypes;
}
