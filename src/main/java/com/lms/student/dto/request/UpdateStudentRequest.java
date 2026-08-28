package com.lms.student.dto.request;

import com.lms.common.domain.Gender;
import com.lms.common.domain.IdProofType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Partial update of a learner record.
 *
 * <p>Every field is optional; a null one is left alone. The exception is
 * {@code enrolments}: a non-null list replaces the whole set, because the form
 * edits batch membership as a group and a merge would make un-enrolling
 * impossible.
 */
@Data
public class UpdateStudentRequest {

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    /**
     * Changing this changes the sign-in identity, not just a contact field.
     * The service rejects an address already held by another account.
     */
    @Email(message = "Enter a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;

    private UUID categoryId;

    private LocalDate admissionDate;

    @Size(max = 512)
    private String photoKey;

    @Size(max = 120, message = "Qualification must not exceed 120 characters")
    private String highestQualification;

    @Size(max = 200, message = "Institution must not exceed 200 characters")
    private String institution;

    private Integer yearOfCompletion;

    @Size(max = 200, message = "Employer must not exceed 200 characters")
    private String employer;

    @PositiveOrZero(message = "Experience cannot be negative")
    private BigDecimal workExperienceYears;

    @Valid
    private AddressRequest address;

    private IdProofType idProofType;

    @Size(max = 60, message = "ID number must not exceed 60 characters")
    private String idProofNumber;

    @Valid
    private EmergencyContactRequest emergencyContact;

    @Valid
    private List<EnrolmentRequest> enrolments;
}
