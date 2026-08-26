package com.lms.instructor.dto.response;

import com.lms.common.domain.Gender;
import com.lms.common.domain.IdProofType;
import com.lms.instructor.entity.EmploymentType;
import com.lms.common.dto.response.AddressResponse;
import com.lms.student.dto.response.BatchResponse;
import com.lms.common.dto.response.EmergencyContactResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class InstructorResponse {

    private UUID id;

    /** The underlying account, for role and status operations. */
    private UUID userId;

    private String fullName;
    private String email;
    private String phone;
    private boolean active;

    /** Locked accounts cannot sign in; this is what the Suspend action toggles. */
    private boolean locked;

    private String employeeCode;
    private LocalDate dateOfBirth;
    private Gender gender;
    private LocalDate joiningDate;
    private EmploymentType employmentType;

    private String photoKey;
    private String photoUrl;

    private String specialization;
    private BigDecimal yearsOfExperience;
    private String bio;

    private String highestQualification;
    private String institution;
    private Integer yearOfCompletion;

    private AddressResponse address;

    private IdProofType idProofType;
    private String idProofNumber;

    private EmergencyContactResponse emergencyContact;

    /**
     * Batches this instructor is assigned to.
     *
     * <p>Populated only on the single-record fetch — resolving it for every row
     * of a list would be a query per instructor.
     */
    private List<BatchResponse> batches;

    private Instant createdAt;
    private Instant updatedAt;
}
