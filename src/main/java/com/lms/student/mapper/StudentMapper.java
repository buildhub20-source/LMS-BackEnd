package com.lms.student.mapper;

import com.lms.common.mapper.ContactMapper;
import com.lms.common.service.StorageService;
import com.lms.common.dto.response.AddressResponse;
import com.lms.common.dto.response.EmergencyContactResponse;
import com.lms.student.dto.response.EnrolmentResponse;
import com.lms.student.dto.response.StudentResponse;
import com.lms.common.domain.Address;
import com.lms.common.domain.EmergencyContact;
import com.lms.student.entity.StudentBatch;
import com.lms.student.entity.StudentProfile;
import com.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/** Maps {@link StudentProfile} entities to API response DTOs. */
@Component
@RequiredArgsConstructor
public class StudentMapper {

    private final StorageService storageService;
    private final ContactMapper contactMapper;

    public StudentResponse toResponse(StudentProfile profile) {
        StudentResponse resp = new StudentResponse();
        resp.setId(profile.getId());

        User user = profile.getUser();
        resp.setUserId(user.getId());
        resp.setFullName(user.getName());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setActive(user.isActive());
        resp.setLocked(user.isLocked());

        resp.setRegistrationNo(profile.getRegistrationNo());
        resp.setDateOfBirth(profile.getDateOfBirth());
        resp.setGender(profile.getGender());
        resp.setAdmissionDate(profile.getAdmissionDate());

        if (profile.getCategory() != null) {
            resp.setCategoryId(profile.getCategory().getId());
            resp.setCategoryName(profile.getCategory().getName());
        }

        resp.setPhotoKey(profile.getPhotoKey());
        resp.setPhotoUrl(publicUrl(profile.getPhotoKey()));

        resp.setHighestQualification(profile.getHighestQualification());
        resp.setInstitution(profile.getInstitution());
        resp.setYearOfCompletion(profile.getYearOfCompletion());
        resp.setEmployer(profile.getEmployer());
        resp.setWorkExperienceYears(profile.getWorkExperienceYears());

        resp.setAddress(contactMapper.toAddressResponse(profile.getAddress()));

        resp.setIdProofType(profile.getIdProofType());
        resp.setIdProofNumber(profile.getIdProofNumber());

        resp.setEmergencyContact(
                contactMapper.toEmergencyContactResponse(profile.getEmergencyContact()));

        // Newest enrolment first — the current batch is what staff look for.
        resp.setEnrolments(profile.getEnrolments().stream()
                .sorted(Comparator.comparing(StudentBatch::getEnrolledOn).reversed())
                .map(this::toEnrolmentResponse)
                .toList());

        resp.setCreatedAt(profile.getCreatedAt());
        resp.setUpdatedAt(profile.getUpdatedAt());
        return resp;
    }

    private EnrolmentResponse toEnrolmentResponse(StudentBatch enrolment) {
        EnrolmentResponse resp = new EnrolmentResponse();
        resp.setId(enrolment.getId());
        resp.setBatchId(enrolment.getBatch().getId());
        resp.setBatchCode(enrolment.getBatch().getCode());
        resp.setBatchName(enrolment.getBatch().getName());
        resp.setBatchStatus(enrolment.getBatch().getStatus());
        resp.setEnrolledOn(enrolment.getEnrolledOn());
        resp.setStatus(enrolment.getStatus());
        resp.setCompletedOn(enrolment.getCompletedOn());
        return resp;
    }



    /** Null when there is no photo, or when public read is not configured. */
    private String publicUrl(String key) {
        return key == null || key.isBlank() ? null : storageService.getPublicUrl(key);
    }
}
