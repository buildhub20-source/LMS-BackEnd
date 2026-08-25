package com.lms.course;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.common.audit.AuditLogRepository;
import com.lms.course.repository.CourseRecordingRepository;
import com.lms.course.repository.CourseRepository;
import com.lms.permission.entity.Permission;
import com.lms.permission.repository.PermissionRepository;
import com.lms.role.entity.Role;
import com.lms.role.repository.RoleRepository;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import com.lms.auth.repository.LoginAttemptRepository;
import com.lms.auth.repository.PasswordResetTokenRepository;
import com.lms.auth.repository.UserSessionRepository;
import com.lms.invitation.repository.InvitationRepository;
import com.lms.user.repository.AccountStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseFlowIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@lms.test";
    private static final String INSTRUCTOR_EMAIL = "instructor@lms.test";
    private static final String PASSWORD = "Password123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseRecordingRepository recordingRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private AccountStatusHistoryRepository accountStatusHistoryRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @Transactional
    void resetAndSeed() {
        auditLogRepository.deleteAll();
        loginAttemptRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        accountStatusHistoryRepository.deleteAll();
        sessionRepository.deleteAll();
        invitationRepository.deleteAll();
        recordingRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        // Seed Permissions
        Permission courseView = createPermission("COURSE_VIEW", "COURSE", "VIEW");
        Permission courseCreate = createPermission("COURSE_CREATE", "COURSE", "CREATE");
        Permission courseUpdate = createPermission("COURSE_UPDATE", "COURSE", "UPDATE");
        Permission courseDelete = createPermission("COURSE_DELETE", "COURSE", "DELETE");
        Permission coursePublish = createPermission("COURSE_PUBLISH", "COURSE", "PUBLISH");
        Permission courseSubmit = createPermission("COURSE_SUBMIT", "COURSE", "SUBMIT");
        Permission courseApprove = createPermission("COURSE_APPROVE", "COURSE", "APPROVE");
        Permission courseReject = createPermission("COURSE_REJECT", "COURSE", "REJECT");
        Permission courseUnpublish = createPermission("COURSE_UNPUBLISH", "COURSE", "UNPUBLISH");
        Permission courseArchive = createPermission("COURSE_ARCHIVE", "COURSE", "ARCHIVE");

        // Seed Roles
        Role adminRole = roleRepository.saveAndFlush(Role.builder()
                .name("ADMIN")
                .permissions(new HashSet<>(Set.of(courseView, courseCreate, courseUpdate, courseDelete,
                        coursePublish, courseApprove, courseReject, courseUnpublish, courseArchive)))
                .build());

        Role instructorRole = roleRepository.saveAndFlush(Role.builder()
                .name("INSTRUCTOR")
                .permissions(new HashSet<>(Set.of(courseView, courseCreate, courseUpdate, courseSubmit)))
                .build());

        // Seed Users
        User adminUser = userRepository.save(User.builder()
                .name("Admin")
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(PASSWORD))
                .active(true)
                .build());
        adminUser.assignRole(adminRole, null);
        userRepository.saveAndFlush(adminUser);

        User instructorUser = userRepository.save(User.builder()
                .name("Instructor")
                .email(INSTRUCTOR_EMAIL)
                .password(passwordEncoder.encode(PASSWORD))
                .active(true)
                .build());
        instructorUser.assignRole(instructorRole, null);
        userRepository.saveAndFlush(instructorUser);
    }

    private Permission createPermission(String name, String resource, String action) {
        return permissionRepository.saveAndFlush(Permission.builder()
                .name(name)
                .resource(resource)
                .action(action)
                .build());
    }

    @Test
    void courseFullLifecycleTest() throws Exception {
        String adminToken = login(ADMIN_EMAIL, PASSWORD).at("/data/tokens/accessToken").asText();
        String instructorToken = login(INSTRUCTOR_EMAIL, PASSWORD).at("/data/tokens/accessToken").asText();

        // 1. Instructor creates a course -> DRAFT
        MvcResult createResult = mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Intro to Java",
                                    "description": "Learn Java basics",
                                    "level": "BEGINNER",
                                    "durationMinutes": 120
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.title").value("Intro to Java"))
                .andReturn();

        String courseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).at("/data/id").asText();

        // 2. Instructor updates the course
        mockMvc.perform(patch("/api/v1/courses/" + courseId)
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "description": "Learn Java basics (Updated)"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("Learn Java basics (Updated)"));

        // 2a. Instructor adds a module
        MvcResult moduleResult = mockMvc.perform(post("/api/v1/courses/" + courseId + "/curriculum/modules")
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Module 1: Basics",
                                    "sortOrder": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Module 1: Basics"))
                .andReturn();
        
        String moduleId = objectMapper.readTree(moduleResult.getResponse().getContentAsString()).at("/data/id").asText();

        // 2b. Instructor adds a lesson
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/curriculum/modules/" + moduleId + "/lessons")
                        .header("Authorization", "Bearer " + instructorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "Lesson 1: Hello World",
                                    "lessonType": "VIDEO",
                                    "sortOrder": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Lesson 1: Hello World"));

        // 3. Instructor submits for review -> PENDING_REVIEW
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/submit")
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));

        // 4. Admin rejects course -> DRAFT
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/reject")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "reason": "Needs more detail"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Needs more detail"));

        // 5. Instructor submits again -> PENDING_REVIEW
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/submit")
                        .header("Authorization", "Bearer " + instructorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_REVIEW"));

        // 6. Admin approves course -> PUBLISHED
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 7. Admin unpublishes course -> UNPUBLISHED
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/unpublish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UNPUBLISHED"));

        // 8. Admin publishes directly -> PUBLISHED
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));

        // 9. Admin archives course -> ARCHIVED
        mockMvc.perform(post("/api/v1/courses/" + courseId + "/archive")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ARCHIVED"));

        // 10. List courses and filter by status
        mockMvc.perform(get("/api/v1/courses?status=ARCHIVED")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(courseId));
        
        // 11. Get Recordings
        mockMvc.perform(get("/api/v1/courses/" + courseId + "/recordings")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void adminCanDeleteDraftCourse() throws Exception {
        String adminToken = login(ADMIN_EMAIL, PASSWORD).at("/data/tokens/accessToken").asText();

        // Admin creates a course -> DRAFT
        MvcResult createResult = mockMvc.perform(post("/api/v1/courses")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "title": "To Be Deleted",
                                    "level": "BEGINNER"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String courseId = objectMapper.readTree(createResult.getResponse().getContentAsString()).at("/data/id").asText();

        // Admin deletes the DRAFT course
        mockMvc.perform(delete("/api/v1/courses/" + courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
                
        // Verify it's gone
        mockMvc.perform(get("/api/v1/courses/" + courseId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    private JsonNode login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }
}
