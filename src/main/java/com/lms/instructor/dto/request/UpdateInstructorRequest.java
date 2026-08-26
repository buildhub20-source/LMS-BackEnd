package com.lms.instructor.dto.request;

import com.lms.common.domain.Gender;
import com.lms.common.domain.IdProofType;
import com.lms.instructor.entity.EmploymentType;
import com.lms.student.dto.request.AddressRequest;
import com.lms.student.dto.request.EmergencyContactRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Partial update of an instructor record; a null field is left alone. */
@Data
public class UpdateInstructorRequest {

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

    private LocalDate joiningDate;

    private EmploymentType employmentType;

    @Size(max = 512)
    private String photoKey;

    @Size(max = 200, message = "Specialization must not exceed 200 characters")
    private String specialization;

    @PositiveOrZero(message = "Experience cannot be negative")
    private BigDecimal yearsOfExperience;

    private String bio;

    @Size(max = 120, message = "Qualification must not exceed 120 characters")
    private String highestQualification;

    @Size(max = 200, message = "Institution must not exceed 200 characters")
    private String institution;

    private Integer yearOfCompletion;

    @Valid
    private AddressRequest address;

    private IdProofType idProofType;

    @Size(max = 60, message = "ID number must not exceed 60 characters")
    private String idProofNumber;

    @Valid
    private EmergencyContactRequest emergencyContact;
}
