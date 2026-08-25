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

        if (lesson.getLessonType() != com.lms.course.entity.LessonType.VIDEO) {
            throw new ApplicationException(ErrorCode.VALIDATION_FAILED, "Upload URLs are only for VIDEO lessons");
        }

        // Create a pending course recording
        com.lms.course.entity.CourseRecording recording = new com.lms.course.entity.CourseRecording();
        recording.setCourseId(courseId);
        recording.setStorageProvider("CLOUDFLARE_R2");
        recording.setFileName(request.getFileName());
        recording.setFileSize(request.getFileSize());
        recording.setMimeType(request.getMimeType());
        recording.setStatus(com.lms.course.entity.RecordingStatus.PENDING);
        // We set createdBy to the current user
        recording.setCreatedBy(com.lms.security.authentication.AuthenticationService.requirePrincipal().getUserId());

        recording = recordingRepository.save(recording);

        // Generate the object key: courses/{courseId}/lessons/{lessonId}/{recordingId}-{filename}
        String extension = "";
        int extIndex = request.getFileName().lastIndexOf('.');
        if (extIndex > 0) {
            extension = request.getFileName().substring(extIndex);
        }
        String key = String.format("courses/%s/lessons/%s/%s%s", courseId, lessonId, recording.getId(), extension);
        recording.setStorageKey(key);
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
}
