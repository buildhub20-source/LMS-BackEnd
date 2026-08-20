package com.lms.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.auth.repository.LoginAttemptRepository;
import com.lms.auth.repository.PasswordResetTokenRepository;
import com.lms.auth.repository.UserSessionRepository;
import com.lms.common.audit.AuditLogRepository;
import com.lms.invitation.repository.InvitationRepository;
import com.lms.permission.entity.Permission;
import com.lms.permission.repository.PermissionRepository;
import com.lms.role.entity.Role;
import com.lms.role.repository.RoleRepository;
import com.lms.support.RecordingMailSender;
import com.lms.user.entity.User;
import com.lms.user.repository.AccountStatusHistoryRepository;
import com.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end coverage of the Module 1 authentication flows.
 *
 * <p>Deliberately not {@code @Transactional}: the login-attempt and audit
 * writes run in their own transactions, so a rolled-back test transaction would
 * not model what actually happens. State is reset explicitly instead.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(RecordingMailSender.Config.class)
class AuthFlowIntegrationTest {

    private static final String ADMIN_EMAIL = "admin@lms.test";
    private static final String ADMIN_PASSWORD = "Adm1n!secret";
    private static final String INVITEE_EMAIL = "ada@lms.test";
    private static final String INVITEE_PASSWORD = "Sup3r!secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RecordingMailSender mailSender;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserSessionRepository sessionRepository;

    @Autowired
    private InvitationRepository invitationRepository;

    @Autowired
    private LoginAttemptRepository loginAttemptRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private AccountStatusHistoryRepository accountStatusHistoryRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    @Transactional
    void resetAndSeed() {
        mailSender.clear();

        auditLogRepository.deleteAll();
        loginAttemptRepository.deleteAll();
        passwordResetTokenRepository.deleteAll();
        accountStatusHistoryRepository.deleteAll();
        sessionRepository.deleteAll();
        invitationRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        Permission invitationCreate = permissionRepository.save(Permission.builder()
                .name("INVITATION_CREATE").resource("INVITATION").action("CREATE").build());
        Permission courseView = permissionRepository.save(Permission.builder()
                .name("COURSE_VIEW").resource("COURSE").action("VIEW").build());

        Role admin = roleRepository.save(Role.builder()
                .name("ADMIN")
                .description("Full platform administration")
                .permissions(new HashSet<>(Set.of(invitationCreate, courseView)))
                .build());

        roleRepository.save(Role.builder()
                .name("STUDENT")
                .description("Consumes published courses")
                .permissions(new HashSet<>(Set.of(courseView)))
                .build());

        User adminUser = userRepository.save(User.builder()
                .name("Platform Administrator")
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .active(true)
                .locked(false)
                .build());
        adminUser.assignRole(admin, null);
        userRepository.saveAndFlush(adminUser);
    }

    @Test
    void anInvitedUserSignsInWithTheTemporaryPasswordAndChoosesTheirOwn() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).at("/data/tokens/accessToken").asText();

        mockMvc.perform(post("/api/v1/invitations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada Lovelace","email":"%s","role":"STUDENT"}
                                """.formatted(INVITEE_EMAIL)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.email").value(INVITEE_EMAIL));

        // The account is live immediately, holding the temporary password.
        assertThat(userRepository.findByEmailIgnoreCase(INVITEE_EMAIL))
                .get()
                .satisfies(user -> {
                    assertThat(user.getPassword()).isNotNull();
                    assertThat(user.isActive()).isTrue();
                });

        String temporaryPassword = temporaryPasswordFor(INVITEE_EMAIL);
        completeOnboarding(temporaryPassword);

        String accessToken = login(INVITEE_EMAIL, INVITEE_PASSWORD).at("/data/tokens/accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("STUDENT"))
                .andExpect(jsonPath("$.data.permissions[0]").value("COURSE_VIEW"));
    }

    @Test
    void afterOnboardingLoginNoLongerDemandsAPasswordChange() throws Exception {
        inviteStudent();
        completeOnboarding(temporaryPasswordFor(INVITEE_EMAIL));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(INVITEE_EMAIL, INVITEE_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(false));
    }

    @Test
    void theTemporaryPasswordStopsWorkingOnceItHasBeenReplaced() throws Exception {
        inviteStudent();
        String temporaryPassword = temporaryPasswordFor(INVITEE_EMAIL);

        completeOnboarding(temporaryPassword);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(INVITEE_EMAIL, temporaryPassword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void thereIsNoPublicAcceptByLinkEndpoint() throws Exception {
        // Removed deliberately; onboarding is temporary-password only. The path
        // is no longer public, so it is rejected before any handler runs.
        mockMvc.perform(post("/api/v1/invitations/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"anything\",\"password\":\"Sup3r!secret\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theInvitationEmailCarriesTheTemporaryPasswordAndNoLink() throws Exception {
        inviteStudent();

        assertThat(mailSender.lastTo(INVITEE_EMAIL).getBody())
                .contains("Temporary password:")
                .doesNotContain("token=");
    }

    @Test
    void refreshRotatesTheTokenAndRetiresThePreviousOne() throws Exception {
        String firstRefresh = login(ADMIN_EMAIL, ADMIN_PASSWORD).at("/data/tokens/refreshToken").asText();

        String rotated = objectMapper.readTree(
                        mockMvc.perform(post("/api/v1/auth/refresh")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.accessToken").exists())
                                .andReturn().getResponse().getContentAsString())
                .at("/data/refreshToken").asText();

        assertThat(rotated).isNotEqualTo(firstRefresh);

        // The retired token must not work again.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + firstRefresh + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));

        // The replacement still does.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotated + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void logoutRevokesTheSessionSoTheRefreshTokenStopsWorking() throws Exception {
        JsonNode tokens = login(ADMIN_EMAIL, ADMIN_PASSWORD);
        String accessToken = tokens.at("/data/tokens/accessToken").asText();
        String refreshToken = tokens.at("/data/tokens/refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedFailuresLockTheAccountAndTheCorrectPasswordThenStopsWorking() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(credentials(ADMIN_EMAIL, "Wr0ng!password")))
                    .andExpect(status().isUnauthorized());
        }

        assertThat(userRepository.findByEmailIgnoreCase(ADMIN_EMAIL)).get()
                .satisfies(user -> assertThat(user.isLocked()).isTrue());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(ADMIN_EMAIL, ADMIN_PASSWORD)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCOUNT_DISABLED"));
    }

    @Test
    void loginWithTheWrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(ADMIN_EMAIL, "Wr0ng!password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void theTemporaryPasswordSignsInButUnlocksNothingUntilItIsReplaced() throws Exception {
        inviteStudent();
        String temporaryPassword = temporaryPasswordFor(INVITEE_EMAIL);

        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(INVITEE_EMAIL, temporaryPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(true))
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(body).at("/data/tokens/accessToken").asText();

        // A permission-guarded endpoint is refused, and so is one that needs
        // nothing more than authentication - the filter closes that gap.
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));

        mockMvc.perform(get("/api/v1/auth/sessions").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("PASSWORD_CHANGE_REQUIRED"));

        // Replacing the password is allowed, and completes onboarding.
        mockMvc.perform(post("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s"}
                                """.formatted(temporaryPassword, INVITEE_PASSWORD)))
                .andExpect(status().isOk());

        String chosen = login(INVITEE_EMAIL, INVITEE_PASSWORD).at("/data/tokens/accessToken").asText();

        // The restriction is gone: this needs only authentication and now works.
        mockMvc.perform(get("/api/v1/auth/sessions").header("Authorization", "Bearer " + chosen))
                .andExpect(status().isOk());

        // Still refused, but now by ordinary authorization rather than the
        // onboarding filter - a STUDENT has no USER_VIEW. The change of error
        // code is what proves the filter is no longer in the way.
        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + chosen))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void theAdministratorNeverSeesTheTemporaryPassword() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).at("/data/tokens/accessToken").asText();

        String response = mockMvc.perform(post("/api/v1/invitations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada Lovelace","email":"%s","role":"STUDENT"}
                                """.formatted(INVITEE_EMAIL)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String temporaryPassword = temporaryPasswordFor(INVITEE_EMAIL);

        assertThat(response).doesNotContain(temporaryPassword);
        assertThat(mailSender.sent()).singleElement()
                .satisfies(message -> assertThat(message.getTo()).isEqualTo(INVITEE_EMAIL));
    }

    @Test
    void aMalformedTokenDoesNotAuthenticate() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theForgotPasswordEndpointDoesNotRevealWhetherAnAccountExists() throws Exception {
        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@lms.test\"}"))
                .andExpect(status().isOk());

        assertThat(mailSender.sent()).isEmpty();
    }

    @Test
    void aPasswordResetSetsTheNewPasswordAndEndsExistingSessions() throws Exception {
        String refreshToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).at("/data/tokens/refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + ADMIN_EMAIL + "\"}"))
                .andExpect(status().isOk());

        String resetToken = mailSender.lastTokenFor(ADMIN_EMAIL);
        String newPassword = "Ren3wed!secret";

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"%s"}
                                """.formatted(resetToken, newPassword)))
                .andExpect(status().isOk());

        // Sessions issued before the reset are gone.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(ADMIN_EMAIL, newPassword)))
                .andExpect(status().isOk());
    }

    private void inviteStudent() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).at("/data/tokens/accessToken").asText();

        mockMvc.perform(post("/api/v1/invitations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada Lovelace","email":"%s","role":"STUDENT"}
                                """.formatted(INVITEE_EMAIL)))
                .andExpect(status().isCreated());
    }

    /** Signs in with the temporary password and replaces it, as an invitee would. */
    private void completeOnboarding(String temporaryPassword) throws Exception {
        String token = login(INVITEE_EMAIL, temporaryPassword).at("/data/tokens/accessToken").asText();

        mockMvc.perform(post("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"%s","newPassword":"%s"}
                                """.formatted(temporaryPassword, INVITEE_PASSWORD)))
                .andExpect(status().isOk());
    }

    /** The temporary password exists only in the invitation email. */
    private String temporaryPasswordFor(String email) {
        String body = mailSender.lastTo(email).getBody();
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("Temporary password: (\\S+)").matcher(body);
        assertThat(matcher.find()).as("temporary password in the invitation email").isTrue();
        return matcher.group(1);
    }

    private JsonNode login(String email, String password) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokens.accessToken").exists())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(body);
    }

    private String credentials(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }
}
