package com.lms.instructor.dto.request;

import com.lms.common.domain.Gender;
import com.lms.common.domain.IdProofType;
import com.lms.instructor.entity.EmploymentType;
import com.lms.student.dto.request.AddressRequest;
import com.lms.student.dto.request.EmergencyContactRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload for onboarding an instructor.
 *
 * <p>Creating an instructor also creates the {@code users} row behind it, which
 * is why name, email and phone are here rather than on the profile: they belong
 * to the account, not the engagement.
 */
@Data
public class CreateInstructorRequest {

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

    // ─── instructor record ───────────────────────────────────────────────────

    @NotBlank(message = "Employee code is required")
    @Size(max = 50, message = "Employee code must not exceed 50 characters")
    private String employeeCode;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    private Gender gender;

    /** Defaults to today when omitted. */
    private LocalDate joiningDate;

    private EmploymentType employmentType;

    @Size(max = 512)
    private String photoKey;

    // ─── teaching ────────────────────────────────────────────────────────────

    @Size(max = 200, message = "Specialization must not exceed 200 characters")
    private String specialization;

    @PositiveOrZero(message = "Experience cannot be negative")
    private BigDecimal yearsOfExperience;

    private String bio;

    // ─── education ───────────────────────────────────────────────────────────

    @Size(max = 120, message = "Qualification must not exceed 120 characters")
    private String highestQualification;

    @Size(max = 200, message = "Institution must not exceed 200 characters")
    private String institution;

    private Integer yearOfCompletion;

    // ─── address and identity ────────────────────────────────────────────────

    @Valid
    private AddressRequest address;

    private IdProofType idProofType;

    @Size(max = 60, message = "ID number must not exceed 60 characters")
    private String idProofNumber;

    @Valid
    private EmergencyContactRequest emergencyContact;
}
