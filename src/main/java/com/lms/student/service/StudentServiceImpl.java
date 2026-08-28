package com.lms.student.service;

import com.lms.common.audit.AuditAction;
import com.lms.common.audit.AuditService;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceAlreadyExistsException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.PageResponse;
import com.lms.assessment.repository.AssessmentAttemptRepository;
import com.lms.assessment.repository.SubmissionRepository;
import com.lms.common.service.StorageService;
import com.lms.invitation.dto.request.CreateInvitationRequest;
import com.lms.invitation.dto.response.InvitationResponse;
import com.lms.invitation.service.InvitationService;
import com.lms.role.constants.SystemRoles;
import com.lms.security.authentication.AuthenticationService;
import com.lms.student.dto.request.AddressRequest;
import com.lms.student.dto.request.CreateStudentRequest;
import com.lms.student.dto.request.EmergencyContactRequest;
import com.lms.student.dto.request.EnrolmentRequest;
import com.lms.student.dto.request.GeneratePhotoUploadUrlRequest;
import com.lms.student.dto.request.UpdateStudentRequest;
import com.lms.student.dto.response.ReferenceItemResponse;
import com.lms.student.dto.response.StudentPhotoUploadUrlResponse;
import com.lms.student.dto.response.StudentReferenceDataResponse;
import com.lms.student.dto.response.StudentResponse;
import com.lms.common.domain.Address;
import com.lms.student.entity.Batch;
import com.lms.common.domain.EmergencyContact;
import com.lms.student.entity.EnrolmentStatus;
import com.lms.common.domain.Gender;
import com.lms.common.domain.IdProofType;
import com.lms.student.entity.StudentBatch;
import com.lms.student.entity.StudentCategory;
import com.lms.student.entity.StudentProfile;
import com.lms.student.mapper.StudentMapper;
import com.lms.student.repository.BatchRepository;
import com.lms.student.repository.StudentBatchRepository;
import com.lms.student.repository.StudentCategoryRepository;
import com.lms.student.repository.StudentProfileRepository;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StudentServiceImpl implements StudentService {

    private static final String RESOURCE = "STUDENT";

    private final StudentProfileRepository studentRepository;
    private final StudentBatchRepository enrolmentRepository;
    private final BatchRepository batchRepository;
    private final StudentCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final SubmissionRepository submissionRepository;
    private final InvitationService invitationService;
    private final BatchService batchService;
    private final StudentMapper studentMapper;
    private final StorageService storageService;
    private final AuditService auditService;

    // ─── CRUD ────────────────────────────────────────────────────────────────

    @Override
    public StudentResponse create(CreateStudentRequest request) {
        UUID actorId = requireCurrentUserId();

        String registrationNo = request.getRegistrationNo().trim();
        if (studentRepository.existsByRegistrationNoIgnoreCase(registrationNo)) {
            throw ResourceAlreadyExistsException.of("Registration number", registrationNo);
        }

        // Creating the account through the invitation flow keeps credential
        // handling in one place: it creates the users row, assigns the role and
        // sends the onboarding mail. It also rejects a duplicate email for us.
        InvitationResponse invitation = invitationService.invite(new CreateInvitationRequest(
                request.getFullName().trim(),
                request.getEmail().trim().toLowerCase(),
                SystemRoles.STUDENT));

        User user = userRepository.findById(invitation.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", invitation.getUserId()));
        user.setPhone(trimToNull(request.getPhone()));

        StudentProfile profile = StudentProfile.builder()
                .user(user)
                .registrationNo(registrationNo)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .category(resolveCategory(request.getCategoryId()))
                .admissionDate(request.getAdmissionDate() == null
                        ? LocalDate.now()
                        : request.getAdmissionDate())
                .photoKey(trimToNull(request.getPhotoKey()))
                .highestQualification(trimToNull(request.getHighestQualification()))
                .institution(trimToNull(request.getInstitution()))
                .yearOfCompletion(request.getYearOfCompletion())
                .employer(trimToNull(request.getEmployer()))
                .workExperienceYears(request.getWorkExperienceYears())
                .address(toAddress(request.getAddress()))
                .idProofType(request.getIdProofType())
                .idProofNumber(trimToNull(request.getIdProofNumber()))
                .emergencyContact(toEmergencyContact(request.getEmergencyContact()))
                .build();

        applyEnrolments(profile, request.getEnrolments());

        StudentProfile saved = studentRepository.save(profile);
        auditService.record(actorId, AuditAction.STUDENT_CREATED, RESOURCE, saved.getId(),
                "Learner admitted: " + registrationNo + " (" + user.getEmail() + ")");

        log.info("Learner {} admitted into {} batch(es)", registrationNo, saved.getEnrolments().size());
        return studentMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StudentResponse findById(UUID id) {
        return studentMapper.toResponse(requireStudent(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentResponse> search(String search, UUID batchId,
                                                EnrolmentStatus enrolmentStatus, Pageable pageable) {

        Page<StudentProfile> page = studentRepository.findAll(
                buildSpec(search, batchId, enrolmentStatus), pageable);
        return PageResponse.from(page, studentMapper::toResponse);
    }

    @Override
    public StudentResponse update(UUID id, UpdateStudentRequest request) {
        UUID actorId = requireCurrentUserId();
        StudentProfile profile = requireStudent(id);

        if (StringUtils.hasText(request.getFullName())) {
            profile.getUser().setName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            profile.getUser().setPhone(trimToNull(request.getPhone()));
        }
        if (StringUtils.hasText(request.getEmail())) {
            applyEmailChange(profile.getUser(), request.getEmail(), actorId);
        }

        if (request.getDateOfBirth() != null)   profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null)        profile.setGender(request.getGender());
        if (request.getCategoryId() != null)    profile.setCategory(resolveCategory(request.getCategoryId()));
        if (request.getAdmissionDate() != null) profile.setAdmissionDate(request.getAdmissionDate());
        if (request.getPhotoKey() != null)      profile.setPhotoKey(trimToNull(request.getPhotoKey()));

        if (request.getHighestQualification() != null) {
            profile.setHighestQualification(trimToNull(request.getHighestQualification()));
        }
        if (request.getInstitution() != null) {
            profile.setInstitution(trimToNull(request.getInstitution()));
        }
        if (request.getYearOfCompletion() != null) {
            profile.setYearOfCompletion(request.getYearOfCompletion());
        }
        if (request.getEmployer() != null) {
            profile.setEmployer(trimToNull(request.getEmployer()));
        }
        if (request.getWorkExperienceYears() != null) {
            profile.setWorkExperienceYears(request.getWorkExperienceYears());
        }

        if (request.getAddress() != null) {
            profile.setAddress(toAddress(request.getAddress()));
        }
        if (request.getIdProofType() != null) {
            profile.setIdProofType(request.getIdProofType());
        }
        if (request.getIdProofNumber() != null) {
            profile.setIdProofNumber(trimToNull(request.getIdProofNumber()));
        }
        if (request.getEmergencyContact() != null) {
            profile.setEmergencyContact(toEmergencyContact(request.getEmergencyContact()));
        }

        // A non-null list replaces the whole set; see UpdateStudentRequest.
        if (request.getEnrolments() != null) {
            profile.getEnrolments().clear();
            applyEnrolments(profile, request.getEnrolments());
        }

        StudentProfile saved = studentRepository.save(profile);
        auditService.record(actorId, AuditAction.STUDENT_UPDATED, RESOURCE, saved.getId(),
                "Learner updated: " + saved.getRegistrationNo());

        return studentMapper.toResponse(saved);
    }

    @Override
    public void delete(UUID id) {
        UUID actorId = requireCurrentUserId();
        StudentProfile profile = requireStudent(id);

        UUID userId = profile.getUser().getId();
        String registrationNo = profile.getRegistrationNo();

        requireNoAssessmentHistory(userId, registrationNo);

        studentRepository.delete(profile);
        // The account exists only to back this record, so it goes too.
        // Enrolments cascade from the profile via the foreign key.
        userRepository.deleteById(userId);

        auditService.record(actorId, AuditAction.STUDENT_DELETED, RESOURCE, id,
                "Learner removed: " + registrationNo);
    }

    // ─── photos ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public StudentPhotoUploadUrlResponse generatePhotoUploadUrl(GeneratePhotoUploadUrlRequest request) {
        String key = "students/photos/" + UUID.randomUUID() + extensionOf(request.getFileName());

        return StudentPhotoUploadUrlResponse.builder()
                .uploadUrl(storageService.generatePresignedUploadUrl(key, request.getMimeType()))
                .photoKey(key)
                .build();
    }

    // ─── reference data ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public StudentReferenceDataResponse referenceData() {
        return StudentReferenceDataResponse.builder()
                // Completed and cancelled batches are not worth offering on an
                // intake form; an edit form still renders whatever is attached.
                .batches(batchService.findOpenForEnrolment())
                .categories(categoryRepository.findAllByOrderBySortOrderAsc().stream()
                        .map(category -> new ReferenceItemResponse(category.getId(), category.getName()))
                        .toList())
                .genders(names(Gender.values()))
                .idProofTypes(names(IdProofType.values()))
                .enrolmentStatuses(names(EnrolmentStatus.values()))
                .build();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    /**
     * Rebuilds the enrolment set from the submitted batches.
     *
     * <p>A learner may sit in several batches at once, but not twice in the
     * same one — that would be a status change, not a second row.
     */
    private void applyEnrolments(StudentProfile profile, List<EnrolmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        Set<UUID> seen = new HashSet<>();
        for (EnrolmentRequest request : requests) {
            if (!seen.add(request.getBatchId())) {
                throw new BusinessRuleException("The same batch was selected more than once");
            }

            Batch batch = batchRepository.findById(request.getBatchId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Batch", request.getBatchId()));

            requireBatchHasRoom(batch, profile.getId());

            profile.addEnrolment(StudentBatch.builder()
                    .batch(batch)
                    .enrolledOn(request.getEnrolledOn() == null ? LocalDate.now() : request.getEnrolledOn())
                    .status(request.getStatus() == null ? EnrolmentStatus.ACTIVE : request.getStatus())
                    .build());
        }
    }

    /**
     * Capacity is advisory on a batch with no limit set, enforced otherwise.
     *
     * <p>Skipped for a learner already counted in this batch, so re-saving an
     * existing record does not trip the check on a batch that is exactly full.
     */
    private void requireBatchHasRoom(Batch batch, UUID currentProfileId) {
        if (batch.getCapacity() == null) {
            return;
        }

        boolean alreadyCounted = currentProfileId != null && studentRepository.findById(currentProfileId)
                .map(existing -> existing.getEnrolments().stream()
                        .anyMatch(enrolment -> enrolment.getBatch().getId().equals(batch.getId())))
                .orElse(false);
        if (alreadyCounted) {
            return;
        }

        long enrolled = enrolmentRepository.countByBatchId(batch.getId());
        if (enrolled >= batch.getCapacity()) {
            throw new BusinessRuleException(
                    "Batch " + batch.getCode() + " is full (" + enrolled + "/" + batch.getCapacity() + ")");
        }
    }

    /**
     * Refuses to delete a learner who has assessment history.
     *
     * <p>{@code assessment_attempts.student_id} and {@code submissions.student_id}
     * are ON DELETE RESTRICT, so removing the account would fail at the database
     * with a constraint error that surfaces as a generic conflict. Checking here
     * turns that into a message naming what actually blocks the delete —
     * and the history is worth keeping, so this is a real rule, not a workaround.
     */
    private void requireNoAssessmentHistory(UUID userId, String registrationNo) {
        long attempts = attemptRepository.countByStudentId(userId);
        long submissions = submissionRepository.countByStudentId(userId);

        if (attempts == 0 && submissions == 0) {
            return;
        }

        throw new BusinessRuleException("Learner " + registrationNo + " has assessment history ("
                + attempts + " attempt(s), " + submissions + " submission(s)) and cannot be deleted. "
                + "Suspend the account instead, which keeps the record and blocks sign-in.");
    }

    private Specification<StudentProfile> buildSpec(String search, UUID batchId,
                                                    EnrolmentStatus enrolmentStatus) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("registrationNo")), pattern),
                        cb.like(cb.lower(root.get("user").get("name")), pattern),
                        cb.like(cb.lower(root.get("user").get("email")), pattern),
                        cb.like(cb.lower(root.get("employer")), pattern)));
            }

            if (batchId != null || enrolmentStatus != null) {
                Join<StudentProfile, StudentBatch> enrolment = root.join("enrolments", JoinType.INNER);
                if (batchId != null) {
                    predicates.add(cb.equal(enrolment.get("batch").get("id"), batchId));
                }
                if (enrolmentStatus != null) {
                    predicates.add(cb.equal(enrolment.get("status"), enrolmentStatus));
                }
                // The join multiplies rows when a learner sits in several batches.
                query.distinct(true);
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Moves the account to a new sign-in address.
     *
     * <p>Lowercased before the comparison and the write: {@code users} carries a
     * CHECK constraint refusing anything else, and lookups go through
     * {@code lower(email)}. A no-op change is allowed through silently so that
     * re-saving an unchanged form does not trip the uniqueness check against the
     * account's own address.
     */
    private void applyEmailChange(User user, String rawEmail, UUID actorId) {
        String email = rawEmail.trim().toLowerCase();
        String current = user.getEmail();

        if (email.equals(current)) {
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ResourceAlreadyExistsException.of("User", email);
        }

        user.setEmail(email);

        // Worth its own audit line: this changes who can sign in as this account,
        // which the generic "learner updated" entry would not make obvious.
        auditService.record(actorId, AuditAction.STUDENT_UPDATED, RESOURCE, user.getId(),
                "Sign-in email changed from " + current + " to " + email);
        log.info("Learner account email changed from {} to {}", current, email);
    }

    private StudentProfile requireStudent(UUID id) {
        return studentRepository.findByIdWithEnrolments(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Student", id));
    }

    private StudentCategory resolveCategory(UUID id) {
        if (id == null) {
            return null;
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Student category", id));
    }

    private static Address toAddress(AddressRequest request) {
        if (request == null) {
            return null;
        }
        return Address.builder()
                .line1(trimToNull(request.getLine1()))
                .line2(trimToNull(request.getLine2()))
                .city(trimToNull(request.getCity()))
                .state(trimToNull(request.getState()))
                .country(trimToNull(request.getCountry()))
                .postalCode(trimToNull(request.getPostalCode()))
                .build();
    }

    private static EmergencyContact toEmergencyContact(EmergencyContactRequest request) {
        if (request == null) {
            return null;
        }
        return EmergencyContact.builder()
                .name(trimToNull(request.getName()))
                .relation(trimToNull(request.getRelation()))
                .phone(trimToNull(request.getPhone()))
                .email(lowerTrimToNull(request.getEmail()))
                .build();
    }

    private static List<String> names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).toList();
    }

    private UUID requireCurrentUserId() {
        return AuthenticationService.requirePrincipal().getUserId();
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot).toLowerCase();
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private static String lowerTrimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : null;
    }
}
