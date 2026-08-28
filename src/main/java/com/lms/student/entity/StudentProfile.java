package com.lms.student.entity;

import com.lms.common.domain.IdProofType;
import com.lms.common.domain.Gender;
import com.lms.common.domain.EmergencyContact;
import com.lms.common.domain.Address;
import com.lms.common.audit.Timestamped;
import com.lms.user.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * The learner record for a training centre or college.
 *
 * <p>Name, email and phone stay on {@link User}: those identify the person and
 * are what authentication uses. Everything here describes them as a learner, and
 * is separate because {@code user_role} is many-to-many — the same person can be
 * both a learner and an instructor.
 *
 * <p>Enrolment lives in {@link StudentBatch} rather than as a column here,
 * because a learner may sit in several batches at once.
 */
@Entity
@Table(name = "student_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentProfile extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Column(name = "registration_no", nullable = false, length = 50)
    private String registrationNo;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    /** Storage key in R2; the public URL is resolved at response time. */
    @Column(name = "photo_key", length = 512)
    private String photoKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private StudentCategory category;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    // ─── education background ────────────────────────────────────────────────

    @Column(name = "highest_qualification", length = 120)
    private String highestQualification;

    @Column(name = "institution", length = 200)
    private String institution;

    @Column(name = "year_of_completion")
    private Integer yearOfCompletion;

    // ─── working life ────────────────────────────────────────────────────────

    @Column(name = "employer", length = 200)
    private String employer;

    /** Fractional years are normal ("2.5"), so this is not an int. */
    @Column(name = "work_experience_years", precision = 4, scale = 1)
    private BigDecimal workExperienceYears;

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

    @Builder.Default
    @OneToMany(mappedBy = "studentProfile", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<StudentBatch> enrolments = new ArrayList<>();

    /** Adds an enrolment, keeping both sides of the association in step. */
    public void addEnrolment(StudentBatch enrolment) {
        enrolment.setStudentProfile(this);
        enrolments.add(enrolment);
    }

    public void removeEnrolment(StudentBatch enrolment) {
        enrolments.remove(enrolment);
        enrolment.setStudentProfile(null);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StudentProfile profile)) {
            return false;
        }
        return id != null && id.equals(profile.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
