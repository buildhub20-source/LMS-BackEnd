package com.lms.student.dto.response;

import com.lms.common.dto.response.EmergencyContactResponse;
import com.lms.common.dto.response.AddressResponse;
import com.lms.common.domain.Gender;
import com.lms.common.domain.IdProofType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class StudentResponse {

    private UUID id;

    /** The underlying account, for role and status operations. */
    private UUID userId;

    private String fullName;
    private String email;
    private String phone;
    private boolean active;

    /** Locked accounts cannot sign in; this is what the Suspend action toggles. */
    private boolean locked;

    private String registrationNo;
    private LocalDate dateOfBirth;
    private Gender gender;
    private LocalDate admissionDate;

    private UUID categoryId;
    private String categoryName;

    private String photoKey;
    private String photoUrl;

    private String highestQualification;
    private String institution;
    private Integer yearOfCompletion;

    private String employer;
    private BigDecimal workExperienceYears;

    private AddressResponse address;

    private IdProofType idProofType;
    private String idProofNumber;

    private EmergencyContactResponse emergencyContact;

    private List<EnrolmentResponse> enrolments;

    private Instant createdAt;
    private Instant updatedAt;
}
