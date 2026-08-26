package com.lms.instructor.service;

import com.lms.assessment.repository.AssessmentRepository;
import com.lms.common.audit.AuditAction;
import com.lms.common.audit.AuditService;
import com.lms.common.domain.Address;
import com.lms.common.domain.EmergencyContact;
import com.lms.common.domain.Gender;
import com.lms.common.domain.IdProofType;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceAlreadyExistsException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.PageResponse;
import com.lms.common.service.StorageService;
import com.lms.instructor.dto.request.CreateInstructorRequest;
import com.lms.instructor.dto.request.UpdateInstructorRequest;
import com.lms.instructor.dto.response.InstructorReferenceDataResponse;
import com.lms.instructor.dto.response.InstructorResponse;
import com.lms.instructor.entity.EmploymentType;
import com.lms.instructor.entity.InstructorProfile;
import com.lms.instructor.mapper.InstructorMapper;
import com.lms.instructor.repository.InstructorProfileRepository;
import com.lms.invitation.dto.request.CreateInvitationRequest;
import com.lms.invitation.dto.response.InvitationResponse;
import com.lms.invitation.service.InvitationService;
import com.lms.role.constants.SystemRoles;
import com.lms.security.authentication.AuthenticationService;
import com.lms.student.dto.request.AddressRequest;
import com.lms.student.dto.request.EmergencyContactRequest;
import com.lms.student.dto.request.GeneratePhotoUploadUrlRequest;
import com.lms.student.dto.response.StudentPhotoUploadUrlResponse;
import com.lms.course.repository.CourseRepository;
import com.lms.student.service.BatchService;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
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
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InstructorServiceImpl implements InstructorService {

    private static final String RESOURCE = "INSTRUCTOR";

    private final InstructorProfileRepository instructorRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AssessmentRepository assessmentRepository;
    private final InvitationService invitationService;
    private final BatchService batchService;
    private final InstructorMapper instructorMapper;
    private final StorageService storageService;
    private final AuditService auditService;

    // ─── CRUD ────────────────────────────────────────────────────────────────

    @Override
    public InstructorResponse create(CreateInstructorRequest request) {
        UUID actorId = requireCurrentUserId();

        String employeeCode = request.getEmployeeCode().trim();
        if (instructorRepository.existsByEmployeeCodeIgnoreCase(employeeCode)) {
            throw ResourceAlreadyExistsException.of("Employee code", employeeCode);
        }

        // Same path as learners: the invitation flow creates the users row,
        // assigns the role and sends the onboarding mail, and rejects a
        // duplicate email for us.
        InvitationResponse invitation = invitationService.invite(new CreateInvitationRequest(
                request.getFullName().trim(),
                request.getEmail().trim().toLowerCase(),
                SystemRoles.INSTRUCTOR));

        User user = userRepository.findById(invitation.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", invitation.getUserId()));
        user.setPhone(trimToNull(request.getPhone()));

        InstructorProfile profile = InstructorProfile.builder()
                .user(user)
                .employeeCode(employeeCode)
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .joiningDate(request.getJoiningDate() == null
                        ? LocalDate.now()
                        : request.getJoiningDate())
                .employmentType(request.getEmploymentType() == null
                        ? EmploymentType.FULL_TIME
                        : request.getEmploymentType())
                .photoKey(trimToNull(request.getPhotoKey()))
                .specialization(trimToNull(request.getSpecialization()))
                .yearsOfExperience(request.getYearsOfExperience())
                .bio(trimToNull(request.getBio()))
                .highestQualification(trimToNull(request.getHighestQualification()))
                .institution(trimToNull(request.getInstitution()))
                .yearOfCompletion(request.getYearOfCompletion())
                .address(toAddress(request.getAddress()))
                .idProofType(request.getIdProofType())
                .idProofNumber(trimToNull(request.getIdProofNumber()))
                .emergencyContact(toEmergencyContact(request.getEmergencyContact()))
                .build();

        InstructorProfile saved = instructorRepository.save(profile);
        auditService.record(actorId, AuditAction.INSTRUCTOR_CREATED, RESOURCE, saved.getId(),
                "Instructor onboarded: " + employeeCode + " (" + user.getEmail() + ")");

        log.info("Instructor {} onboarded as {}", employeeCode, saved.getEmploymentType());
        return instructorMapper.toResponse(saved, List.of());
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorResponse findById(UUID id) {
        InstructorProfile profile = requireInstructor(id);
        // Assigned batches hang off batches.instructor_id, keyed by the account.
        return instructorMapper.toResponse(
                profile, batchService.findByInstructor(profile.getUser().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InstructorResponse> search(String search, EmploymentType employmentType,
                                                   Pageable pageable) {

        Page<InstructorProfile> page = instructorRepository.findAll(
                buildSpec(search, employmentType), pageable);
        return PageResponse.from(page, instructorMapper::toResponse);
    }

    @Override
    public InstructorResponse update(UUID id, UpdateInstructorRequest request) {
        UUID actorId = requireCurrentUserId();
        InstructorProfile profile = requireInstructor(id);

        if (StringUtils.hasText(request.getFullName())) {
            profile.getUser().setName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            profile.getUser().setPhone(trimToNull(request.getPhone()));
        }
        if (StringUtils.hasText(request.getEmail())) {
            applyEmailChange(profile.getUser(), request.getEmail(), actorId);
        }

        if (request.getDateOfBirth() != null)     profile.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender() != null)          profile.setGender(request.getGender());
        if (request.getJoiningDate() != null)     profile.setJoiningDate(request.getJoiningDate());
        if (request.getEmploymentType() != null)  profile.setEmploymentType(request.getEmploymentType());
        if (request.getPhotoKey() != null)        profile.setPhotoKey(trimToNull(request.getPhotoKey()));

        if (request.getSpecialization() != null) {
            profile.setSpecialization(trimToNull(request.getSpecialization()));
        }
        if (request.getYearsOfExperience() != null) {
            profile.setYearsOfExperience(request.getYearsOfExperience());
        }
        if (request.getBio() != null) {
            profile.setBio(trimToNull(request.getBio()));
        }
        if (request.getHighestQualification() != null) {
            profile.setHighestQualification(trimToNull(request.getHighestQualification()));
        }
        if (request.getInstitution() != null) {
            profile.setInstitution(trimToNull(request.getInstitution()));
        }
        if (request.getYearOfCompletion() != null) {
            profile.setYearOfCompletion(request.getYearOfCompletion());
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

        InstructorProfile saved = instructorRepository.save(profile);
        auditService.record(actorId, AuditAction.INSTRUCTOR_UPDATED, RESOURCE, saved.getId(),
                "Instructor updated: " + saved.getEmployeeCode());

        return instructorMapper.toResponse(
                saved, batchService.findByInstructor(saved.getUser().getId()));
    }

    @Override
    public void delete(UUID id) {
        UUID actorId = requireCurrentUserId();
        InstructorProfile profile = requireInstructor(id);

        UUID userId = profile.getUser().getId();
        String employeeCode = profile.getEmployeeCode();

        requireNothingDependsOnThem(userId, employeeCode);

        instructorRepository.delete(profile);
        // The account exists only to back this record, so it goes too. Batches
        // keep running: batches.instructor_id is ON DELETE SET NULL, which
        // leaves them unassigned rather than deleting scheduled teaching.
        userRepository.deleteById(userId);

        auditService.record(actorId, AuditAction.INSTRUCTOR_DELETED, RESOURCE, id,
                "Instructor removed: " + employeeCode);
    }

    // ─── photos ──────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public StudentPhotoUploadUrlResponse generatePhotoUploadUrl(GeneratePhotoUploadUrlRequest request) {
        String key = "instructors/photos/" + UUID.randomUUID() + extensionOf(request.getFileName());

        return StudentPhotoUploadUrlResponse.builder()
                .uploadUrl(storageService.generatePresignedUploadUrl(key, request.getMimeType()))
                .photoKey(key)
                .build();
    }

    // ─── reference data ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public InstructorReferenceDataResponse referenceData() {
        return InstructorReferenceDataResponse.builder()
                .genders(names(Gender.values()))
                .idProofTypes(names(IdProofType.values()))
                .employmentTypes(names(EmploymentType.values()))
                .build();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    /**
     * Moves the account to a new sign-in address.
     *
     * <p>Lowercased before the comparison and the write: {@code users} carries a
     * CHECK constraint refusing anything else. A no-op change passes silently so
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

        auditService.record(actorId, AuditAction.INSTRUCTOR_UPDATED, RESOURCE, user.getId(),
                "Sign-in email changed from " + current + " to " + email);
        log.info("Instructor account email changed from {} to {}", current, email);
    }

    /**
     * Refuses to delete an instructor who still owns teaching material.
     *
     * <p>{@code courses.created_by} and {@code courses.instructor_id} carry no
     * delete rule (so NO ACTION), and {@code assessments.created_by} is RESTRICT.
     * Removing the account would fail at the database with a constraint error
     * that surfaces as a generic conflict; checking here names what blocks it.
     *
     * <p>Batches are deliberately not part of this check: their
     * {@code instructor_id} is ON DELETE SET NULL, so they survive unassigned
     * rather than blocking the delete or vanishing.
     */
    private void requireNothingDependsOnThem(UUID userId, String employeeCode) {
        long authored = courseRepository.countByCreatedBy(userId);
        long teaching = courseRepository.countByInstructorId(userId);
        long assessments = assessmentRepository.countByCreatedBy(userId);

        if (authored == 0 && teaching == 0 && assessments == 0) {
            return;
        }

        List<String> blockers = new ArrayList<>();
        if (authored > 0)    blockers.add(authored + " course(s) they created");
        if (teaching > 0)    blockers.add(teaching + " course(s) assigned to them");
        if (assessments > 0) blockers.add(assessments + " assessment(s) they created");

        throw new BusinessRuleException("Instructor " + employeeCode + " still owns "
                + String.join(", ", blockers) + " and cannot be deleted. Reassign that work, "
                + "or suspend the account instead to keep the record and block sign-in.");
    }

    private Specification<InstructorProfile> buildSpec(String search, EmploymentType employmentType) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("employeeCode")), pattern),
                        cb.like(cb.lower(root.get("user").get("name")), pattern),
                        cb.like(cb.lower(root.get("user").get("email")), pattern),
                        cb.like(cb.lower(root.get("specialization")), pattern)));
            }
            if (employmentType != null) {
                predicates.add(cb.equal(root.get("employmentType"), employmentType));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private InstructorProfile requireInstructor(UUID id) {
        return instructorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Instructor", id));
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
