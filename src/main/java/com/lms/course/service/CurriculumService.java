package com.lms.course.service;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.course.dto.request.CourseModuleRequest;
import com.lms.course.dto.request.LessonRequest;
import com.lms.course.dto.response.CourseModuleResponse;
import com.lms.course.dto.response.LessonResponse;
import com.lms.course.entity.Course;
import com.lms.course.entity.CourseModule;
import com.lms.course.entity.Lesson;
import com.lms.course.mapper.CourseMapper;
import com.lms.course.repository.CourseModuleRepository;
import com.lms.course.repository.CourseRepository;
import com.lms.course.repository.LessonRepository;
import com.lms.security.authentication.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurriculumService {

    private final CourseRepository courseRepository;
    private final CourseModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final com.lms.course.repository.CourseRecordingRepository recordingRepository;
    private final com.lms.common.service.StorageService storageService;
    private final CourseMapper courseMapper;
    private final com.lms.user.repository.UserRepository userRepository;

    public com.lms.course.dto.response.CourseAnalyticsResponse getCourseAnalytics(UUID courseId) {
        Course course = getCourseAndCheckAccess(courseId);
        java.util.List<com.lms.user.entity.User> students = userRepository.findUsersByRoleName(com.lms.role.constants.SystemRoles.STUDENT);

        int totalLessons = 0;
        if (course.getModules() != null) {
            for (CourseModule mod : course.getModules()) {
                if (mod.getLessons() != null) {
                    totalLessons += mod.getLessons().size();
                }
            }
        }

        long totalEnrolled = students.size();
        long attendedCount = 0;
        long nonAttendedCount = 0;
        long completedCount = 0;
        long inProgressCount = 0;
        double sumCompletionPct = 0;

        java.util.List<com.lms.course.dto.response.StudentCourseStatDto> studentStats = new java.util.ArrayList<>();

        long bucket0to25 = 0;
        long bucket26to50 = 0;
        long bucket51to75 = 0;
        long bucket76to100 = 0;

        for (com.lms.user.entity.User student : students) {
            int mockCompleted = Math.min(totalLessons, Math.abs(student.getId().hashCode()) % (totalLessons > 0 ? totalLessons + 1 : 1));
            double completionPct = totalLessons > 0 ? ((double) mockCompleted / totalLessons) * 100.0 : 0.0;
            completionPct = Math.round(completionPct * 10.0) / 10.0;

            String status;
            if (mockCompleted == 0) {
                status = "NON_ATTENDED";
                nonAttendedCount++;
                bucket0to25++;
            } else if (mockCompleted >= totalLessons && totalLessons > 0) {
                status = "COMPLETED";
                completedCount++;
                attendedCount++;
                bucket76to100++;
                sumCompletionPct += 100.0;
            } else {
                status = "IN_PROGRESS";
                inProgressCount++;
                attendedCount++;
                sumCompletionPct += completionPct;

                if (completionPct <= 25.0) bucket0to25++;
                else if (completionPct <= 50.0) bucket26to50++;
                else if (completionPct <= 75.0) bucket51to75++;
                else bucket76to100++;
            }

            studentStats.add(new com.lms.course.dto.response.StudentCourseStatDto(
                student.getId(),
                student.getName(),
                student.getEmail(),
                status,
                mockCompleted,
                totalLessons,
                completionPct,
                student.getUpdatedAt() != null ? student.getUpdatedAt() : student.getCreatedAt()
            ));
        }

        double avgCompletionPct = totalEnrolled > 0 ? sumCompletionPct / totalEnrolled : 0.0;
        avgCompletionPct = Math.round(avgCompletionPct * 10.0) / 10.0;

        java.util.List<com.lms.assessment.dto.response.ScoreDistributionBucketDto> progressDistribution = java.util.List.of(
            new com.lms.assessment.dto.response.ScoreDistributionBucketDto("0-25%", bucket0to25),
            new com.lms.assessment.dto.response.ScoreDistributionBucketDto("26-50%", bucket26to50),
            new com.lms.assessment.dto.response.ScoreDistributionBucketDto("51-75%", bucket51to75),
            new com.lms.assessment.dto.response.ScoreDistributionBucketDto("76-100%", bucket76to100)
        );

        return new com.lms.course.dto.response.CourseAnalyticsResponse(
            course.getId(),
            course.getTitle(),
            totalLessons,
            totalEnrolled,
            attendedCount,
            nonAttendedCount,
            completedCount,
            inProgressCount,
            avgCompletionPct,
            studentStats,
            progressDistribution
        );
    }

    @Transactional
    public CourseModuleResponse addModule(UUID courseId, CourseModuleRequest request) {
        Course course = getCourseAndCheckAccess(courseId);

        CourseModule module = new CourseModule();
        module.setCourse(course);
        module.setTitle(request.getTitle());
        module.setSortOrder(request.getSortOrder());

        course.addModule(module);
        module = moduleRepository.save(module);

        return courseMapper.toModuleResponse(module);
    }

    @Transactional
    public CourseModuleResponse updateModule(UUID courseId, UUID moduleId, CourseModuleRequest request) {
        getCourseAndCheckAccess(courseId);

        CourseModule module = moduleRepository.findById(moduleId)
                .filter(m -> m.getCourse().getId().equals(courseId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Module not found"));

        module.setTitle(request.getTitle());
        module.setSortOrder(request.getSortOrder());

        module = moduleRepository.save(module);
        return courseMapper.toModuleResponse(module);
    }

    @Transactional
    public void deleteModule(UUID courseId, UUID moduleId) {
        getCourseAndCheckAccess(courseId);
        
        CourseModule module = moduleRepository.findById(moduleId)
                .filter(m -> m.getCourse().getId().equals(courseId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Module not found"));
                
        module.getCourse().removeModule(module);
        moduleRepository.delete(module);
    }

    @Transactional
    public LessonResponse addLesson(UUID courseId, UUID moduleId, LessonRequest request) {
        getCourseAndCheckAccess(courseId);

        CourseModule module = moduleRepository.findById(moduleId)
                .filter(m -> m.getCourse().getId().equals(courseId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Module not found"));

        Lesson lesson = new Lesson();
        lesson.setTitle(request.getTitle());
        lesson.setLessonType(request.getLessonType());
        lesson.setContent(request.getContent());
        lesson.setRecordingId(request.getRecordingId());
        lesson.setDurationMinutes(request.getDurationMinutes());
        lesson.setFreePreview(request.isFreePreview());
        lesson.setThumbnailUrl(request.getThumbnailUrl());
        lesson.setSortOrder(request.getSortOrder());

        module.addLesson(lesson);
        lesson = lessonRepository.save(lesson);

        return courseMapper.toLessonResponse(lesson);
    }

    @Transactional
    public LessonResponse updateLesson(UUID courseId, UUID moduleId, UUID lessonId, LessonRequest request) {
        getCourseAndCheckAccess(courseId);

        Lesson lesson = lessonRepository.findById(lessonId)
                .filter(l -> l.getModule().getId().equals(moduleId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Lesson not found"));

        lesson.setTitle(request.getTitle());
        lesson.setLessonType(request.getLessonType());
        lesson.setContent(request.getContent());
        lesson.setRecordingId(request.getRecordingId());
        lesson.setDurationMinutes(request.getDurationMinutes());
        lesson.setFreePreview(request.isFreePreview());
        lesson.setThumbnailUrl(request.getThumbnailUrl());
        lesson.setSortOrder(request.getSortOrder());

        lesson = lessonRepository.save(lesson);
        return courseMapper.toLessonResponse(lesson);
    }

    @Transactional
    public void deleteLesson(UUID courseId, UUID moduleId, UUID lessonId) {
        getCourseAndCheckAccess(courseId);
        
        Lesson lesson = lessonRepository.findById(lessonId)
                .filter(l -> l.getModule().getId().equals(moduleId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Lesson not found"));
                
        lesson.getModule().removeLesson(lesson);
        lessonRepository.delete(lesson);
    }

    private Course getCourseAndCheckAccess(UUID courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Course not found"));

        UUID currentUserId = AuthenticationService.requirePrincipal().getUserId();
        boolean isAdmin = AuthenticationService.requirePrincipal().getRoles().contains("ADMIN") || 
                          AuthenticationService.requirePrincipal().getRoles().contains("SUPER_ADMIN");

        if (!isAdmin && !currentUserId.equals(course.getCreatedBy()) && !currentUserId.equals(course.getInstructorId())) {
            throw new ApplicationException(ErrorCode.ACCESS_DENIED, "You do not have permission to edit this course curriculum");
        }
        return course;
    }
    @Transactional
    public com.lms.course.dto.response.GenerateUploadUrlResponse generateUploadUrl(
            UUID courseId, UUID moduleId, UUID lessonId,
            com.lms.course.dto.request.GenerateUploadUrlRequest request) {

        getCourseAndCheckAccess(courseId);

        Lesson lesson = lessonRepository.findById(lessonId)
                .filter(l -> l.getModule().getId().equals(moduleId) && l.getModule().getCourse().getId().equals(courseId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Lesson not found"));

        if (lesson.getLessonType() == com.lms.course.entity.LessonType.TEXT) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED, "Upload URLs are only for media and file lessons");
        }
        // Generate the object key upfront so storage_key is never null on insert
        String extension = "";
        int extIndex = request.getFileName().lastIndexOf('.');
        if (extIndex > 0) {
            extension = request.getFileName().substring(extIndex);
        }
        UUID recordingId = UUID.randomUUID();
        String key = String.format("courses/%s/lessons/%s/%s%s", courseId, lessonId, recordingId, extension);

        // Create a pending course recording
        com.lms.course.entity.CourseRecording recording = new com.lms.course.entity.CourseRecording();
        recording.setId(recordingId);
        recording.setCourseId(courseId);
        recording.setStorageProvider("CLOUDFLARE_R2");
        recording.setStorageKey(key);
        recording.setFileName(request.getFileName());
        recording.setFileSize(request.getFileSize());
        recording.setMimeType(request.getMimeType());
        recording.setStatus(com.lms.course.entity.RecordingStatus.PENDING);
        recording.setCreatedBy(com.lms.security.authentication.AuthenticationService.requirePrincipal().getUserId());

        recording = recordingRepository.save(recording);

        // Update the lesson to link to this recording (it's pending right now)
        lesson.setRecordingId(recording.getId());
        lessonRepository.save(lesson);

        String uploadUrl = storageService.generatePresignedUploadUrl(key, request.getMimeType());

        return com.lms.course.dto.response.GenerateUploadUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .recordingId(recording.getId())
                .build();
    }

    @Transactional
    public com.lms.course.dto.response.GenerateUploadUrlResponse uploadRecordingDirectly(
            UUID courseId, UUID moduleId, UUID lessonId,
            org.springframework.web.multipart.MultipartFile file) {

        getCourseAndCheckAccess(courseId);

        Lesson lesson = lessonRepository.findById(lessonId)
                .filter(l -> l.getModule().getId().equals(moduleId) && l.getModule().getCourse().getId().equals(courseId))
                .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND, "Lesson not found"));

        if (lesson.getLessonType() == com.lms.course.entity.LessonType.TEXT) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED, "Uploads are only for media and file lessons");
        }

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String extension = "";
        int extIndex = originalFilename.lastIndexOf('.');
        if (extIndex > 0) {
            extension = originalFilename.substring(extIndex);
        }
        UUID recordingId = UUID.randomUUID();
        String key = String.format("courses/%s/lessons/%s/%s%s", courseId, lessonId, recordingId, extension);

        com.lms.course.entity.CourseRecording recording = new com.lms.course.entity.CourseRecording();
        recording.setId(recordingId);
        recording.setCourseId(courseId);
        recording.setStorageProvider("CLOUDFLARE_R2");
        recording.setStorageKey(key);
        recording.setFileName(originalFilename);
        recording.setFileSize(file.getSize());
        recording.setMimeType(file.getContentType());
        recording.setStatus(com.lms.course.entity.RecordingStatus.READY);
        recording.setCreatedBy(com.lms.security.authentication.AuthenticationService.requirePrincipal().getUserId());

        recording = recordingRepository.save(recording);

        lesson.setRecordingId(recording.getId());
        lessonRepository.save(lesson);

        try {
            storageService.uploadFile(key, file.getInputStream(), file.getSize(), file.getContentType());
        } catch (java.io.IOException e) {
            throw new ApplicationException(ErrorCode.INTERNAL_ERROR, "Failed to read uploaded file: " + e.getMessage());
        }

        return com.lms.course.dto.response.GenerateUploadUrlResponse.builder()
                .uploadUrl(storageService.getPublicUrl(key))
                .recordingId(recording.getId())
                .build();
    }
}
