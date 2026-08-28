package com.lms.instructor.entity;

import com.lms.common.audit.Timestamped;
import com.lms.common.domain.Address;
import com.lms.common.domain.EmergencyContact;
import com.lms.common.domain.Gender;
import com.lms.common.domain.IdProofType;
import com.lms.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * The instructor record for a training centre or college.
 *
 * <p>Mirrors {@link com.lms.student.entity.StudentProfile}: {@link User} stays
 * the identity record and this holds the role-specific detail. Kept as its own
 * table rather than a shared person profile because {@code user_role} is
 * many-to-many — the same person can be both a learner and an instructor.
 *
 * <p>Assigned batches are not modelled here: {@code lms.batches} already carries
 * {@code instructor_id}, so they are a query rather than a second relationship.
 */
@Entity
@Table(name = "instructor_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorProfile extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    /** Storage key in R2; the public URL is resolved at response time. */
    @Column(name = "photo_key", length = 512)
    private String photoKey;

    @Column(name = "joining_date")
    private LocalDate joiningDate;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type", nullable = false, length = 20)
    private EmploymentType employmentType = EmploymentType.FULL_TIME;

    // ─── teaching ────────────────────────────────────────────────────────────

    @Column(name = "specialization", length = 200)
    private String specialization;

    /** Fractional years are normal ("7.5"), so this is not an int. */
    @Column(name = "years_of_experience", precision = 4, scale = 1)
    private BigDecimal yearsOfExperience;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    // ─── education background ────────────────────────────────────────────────

    @Column(name = "highest_qualification", length = 120)
    private String highestQualification;

    @Column(name = "institution", length = 200)
    private String institution;

    @Column(name = "year_of_completion")
    private Integer yearOfCompletion;

    // ─── address and identity ────────────────────────────────────────────────

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(name = "id_proof_type", length = 30)
    private IdProofType idProofType;

    @Column(name = "id_proof_number", length = 60)
    private String idProofNumber;

    @Embedded
    private EmergencyContact emergencyContact;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof InstructorProfile profile)) {
            return false;
        }
        return id != null && id.equals(profile.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
