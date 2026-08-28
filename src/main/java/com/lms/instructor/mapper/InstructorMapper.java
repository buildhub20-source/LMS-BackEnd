package com.lms.instructor.mapper;

import com.lms.common.mapper.ContactMapper;
import com.lms.common.service.StorageService;
import com.lms.instructor.dto.response.InstructorResponse;
import com.lms.instructor.entity.InstructorProfile;
import com.lms.student.dto.response.BatchResponse;
import com.lms.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Maps {@link InstructorProfile} entities to API response DTOs. */
@Component
@RequiredArgsConstructor
public class InstructorMapper {

    private final StorageService storageService;
    private final ContactMapper contactMapper;

    /**
     * @param batches assigned batches, or null on list responses where resolving
     *                them per row would be a query per instructor
     */
    public InstructorResponse toResponse(InstructorProfile profile, List<BatchResponse> batches) {
        InstructorResponse resp = new InstructorResponse();
        resp.setId(profile.getId());

        User user = profile.getUser();
        resp.setUserId(user.getId());
        resp.setFullName(user.getName());
        resp.setEmail(user.getEmail());
        resp.setPhone(user.getPhone());
        resp.setActive(user.isActive());
        resp.setLocked(user.isLocked());

        resp.setEmployeeCode(profile.getEmployeeCode());
        resp.setDateOfBirth(profile.getDateOfBirth());
        resp.setGender(profile.getGender());
        resp.setJoiningDate(profile.getJoiningDate());
        resp.setEmploymentType(profile.getEmploymentType());

        resp.setPhotoKey(profile.getPhotoKey());
        resp.setPhotoUrl(publicUrl(profile.getPhotoKey()));

        resp.setSpecialization(profile.getSpecialization());
        resp.setYearsOfExperience(profile.getYearsOfExperience());
        resp.setBio(profile.getBio());

        resp.setHighestQualification(profile.getHighestQualification());
        resp.setInstitution(profile.getInstitution());
        resp.setYearOfCompletion(profile.getYearOfCompletion());

        resp.setAddress(contactMapper.toAddressResponse(profile.getAddress()));

        resp.setIdProofType(profile.getIdProofType());
        resp.setIdProofNumber(profile.getIdProofNumber());

        resp.setEmergencyContact(
                contactMapper.toEmergencyContactResponse(profile.getEmergencyContact()));

        resp.setBatches(batches);

        resp.setCreatedAt(profile.getCreatedAt());
        resp.setUpdatedAt(profile.getUpdatedAt());
        return resp;
    }

    /** List responses: identity and engagement only, no batch lookup. */
    public InstructorResponse toResponse(InstructorProfile profile) {
        return toResponse(profile, null);
    }

    /** Null when there is no photo, or when public read is not configured. */
    private String publicUrl(String key) {
        return key == null || key.isBlank() ? null : storageService.getPublicUrl(key);
    }
}
