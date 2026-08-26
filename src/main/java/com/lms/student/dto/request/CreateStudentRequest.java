package com.lms.student.dto.request;

import com.lms.common.domain.Gender;
import com.lms.common.domain.IdProofType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Payload for admitting a learner.
 *
 * <p>Creating a learner also creates the {@code users} row behind it, which is
 * why name, email and phone are here rather than on the profile: they belong to
 * the account, not the enrolment.
 */
@Data
public class CreateStudentRequest {

    // ─── account ─────────────────────────────────────────────────────────────

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "Phone is required")
    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    // ─── learner record ──────────────────────────────────────────────────────

    @NotBlank(message = "Registration number is required")
    @Size(max = 50, message = "Registration number must not exceed 50 characters")
    private String registrationNo;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;

    private UUID categoryId;

    /** Defaults to today when omitted. */
    private LocalDate admissionDate;

    /** Storage key returned by the photo upload-url endpoint. */
    @Size(max = 512)
    private String photoKey;

    // ─── education and work ──────────────────────────────────────────────────

    @Size(max = 120, message = "Qualification must not exceed 120 characters")
    private String highestQualification;

    @Size(max = 200, message = "Institution must not exceed 200 characters")
    private String institution;

    private Integer yearOfCompletion;

    @Size(max = 200, message = "Employer must not exceed 200 characters")
    private String employer;

    @PositiveOrZero(message = "Experience cannot be negative")
    private BigDecimal workExperienceYears;

    // ─── address and identity ────────────────────────────────────────────────

    @Valid
    private AddressRequest address;

    private IdProofType idProofType;

    @Size(max = 60, message = "ID number must not exceed 60 characters")
    private String idProofNumber;

    @Valid
    private EmergencyContactRequest emergencyContact;

    /** Batches to enrol into; a learner may join several at once. */
    @Valid
    private List<EnrolmentRequest> enrolments = new ArrayList<>();
}
