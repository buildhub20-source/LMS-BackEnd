package com.lms.announcement;

import com.lms.announcement.repository.AnnouncementRepository;
import com.lms.notification.repository.NotificationRepository;
import com.lms.role.entity.Role;
import com.lms.role.repository.RoleRepository;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AnnouncementIntegrationTest {

    private static final String URL = "/api/v1/announcements";

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired AnnouncementRepository announcements;
    @Autowired NotificationRepository notifications;

    private LmsUserDetails instructor;
    private LmsUserDetails otherInstructor;
    private LmsUserDetails admin;
    private LmsUserDetails student;
    private UUID inactiveStudentId;

    @BeforeEach
    void seed() {
        announcements.deleteAll();
        instructor = principal(saveUser("Ada", "INSTRUCTOR", true), "INSTRUCTOR");
        otherInstructor = principal(saveUser("Grace", "INSTRUCTOR", true), "INSTRUCTOR");
        admin = principal(saveUser("Admin", "ADMIN", true), "ADMIN");
        student = principal(saveUser("Sam", "STUDENT", true), "STUDENT");
        inactiveStudentId = saveUser("Pending", "STUDENT", false).getId();
    }

    @Test
    void anInstructorPublishesAndActiveStudentsAreNotified() throws Exception {
        mockMvc.perform(post(URL).with(as(instructor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Lab moved\",\"message\":\"Thursday's lab is in room 204.\",\"type\":\"WARNING\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("Lab moved"))
                .andExpect(jsonPath("$.data.type").value("WARNING"))
                .andExpect(jsonPath("$.data.active").value(true));

        assertThat(notifications.findByUserIdOrderByCreatedAtDesc(student.getUserId(), Pageable.unpaged()))
                .singleElement()
                .satisfies(n -> assertThat(n.getTitle()).isEqualTo("Lab moved"));
        assertThat(notifications.countByUserIdAndReadFalse(inactiveStudentId)).isZero();
    }

    @Test
    void typeDefaultsToInfo() throws Exception {
        mockMvc.perform(post(URL).with(as(instructor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hello\",\"message\":\"Welcome back.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("INFO"));
    }

    @Test
    void studentsCannotPublishOrListEverything() throws Exception {
        mockMvc.perform(post(URL).with(as(student)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hi\",\"message\":\"Free pizza\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL).with(as(student))).andExpect(status().isForbidden());
    }

    @Test
    void instructorsSeeAndChangeOnlyTheirOwn() throws Exception {
        String id = publish(instructor, "Mine");
        publish(otherInstructor, "Theirs");

        mockMvc.perform(get(URL).with(as(instructor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Mine"));

        mockMvc.perform(post(URL + "/" + id + "/toggle").param("active", "false").with(as(otherInstructor)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(URL + "/" + id).with(as(otherInstructor)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(URL).with(as(admin)))
                .andExpect(jsonPath("$.data.length()").value(2));
        mockMvc.perform(delete(URL + "/" + id).with(as(admin))).andExpect(status().isOk());
    }

    @Test
    void activeListHidesSwitchedOffAndExpiredAnnouncements() throws Exception {
        String off = publish(instructor, "Switched off");
        publish(instructor, "Visible");
        String expiring = publish(instructor, "Expiring");

        mockMvc.perform(post(URL + "/" + off + "/toggle").param("active", "false").with(as(instructor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
        announcements.findById(UUID.fromString(expiring)).orElseThrow()
                .setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get(URL + "/active").with(as(student)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Visible"));
    }

    @Test
    void invalidInputIsRejected() throws Exception {
        mockMvc.perform(post(URL).with(as(instructor)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \",\"message\":\"x\",\"expiresAt\":\"2020-01-01T00:00:00Z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void anonymousCallersAreTurnedAway() throws Exception {
        mockMvc.perform(get(URL + "/active")).andExpect(status().isUnauthorized());
    }

    private String publish(LmsUserDetails who, String title) throws Exception {
        String body = mockMvc.perform(post(URL).with(as(who)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"message\":\"Details.\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.data.id");
    }

    private User saveUser(String name, String roleName, boolean active) {
        Role role = roles.findByName(roleName)
                .orElseGet(() -> roles.save(Role.builder().name(roleName).description(roleName).build()));
        User user = users.save(User.builder().name(name).email(UUID.randomUUID() + "@lms.test")
                .password("unused").active(active).locked(false).build());
        user.assignRole(role, null);
        return users.saveAndFlush(user);
    }

    private static LmsUserDetails principal(User user, String role) {
        return LmsUserDetails.fromClaims(user.getId(), user.getEmail(), Set.of(role), Set.of(), UUID.randomUUID());
    }

    private static RequestPostProcessor as(LmsUserDetails who) {
        return authentication(new UsernamePasswordAuthenticationToken(who, null, who.getAuthorities()));
    }
}
