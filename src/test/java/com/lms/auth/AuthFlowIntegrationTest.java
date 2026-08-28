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
    void anInvitedUserAcceptsTheLinkAndChoosesTheirOwnPassword() throws Exception {
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

        // The account exists but holds no credentials until the link is accepted.
        assertThat(userRepository.findByEmailIgnoreCase(INVITEE_EMAIL))
                .get()
                .satisfies(user -> {
                    assertThat(user.getPassword()).isNull();
                    assertThat(user.isActive()).isFalse();
                });

        acceptInvitation(invitationTokenFor(INVITEE_EMAIL), INVITEE_PASSWORD);

        String accessToken = login(INVITEE_EMAIL, INVITEE_PASSWORD).at("/data/tokens/accessToken").asText();

        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("STUDENT"))
                .andExpect(jsonPath("$.data.permissions[0]").value("COURSE_VIEW"));
    }

    @Test
    void anUnacceptedInviteeCannotSignInAtAll() throws Exception {
        inviteStudent();

        // There is no credential to try: the row carries no password until the
        // link is accepted, so there is no window where a half-onboarded
        // account can authenticate.
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(INVITEE_EMAIL, INVITEE_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void afterAcceptingLoginNoLongerDemandsAPasswordChange() throws Exception {
        inviteStudent();
        acceptInvitation(invitationTokenFor(INVITEE_EMAIL), INVITEE_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(INVITEE_EMAIL, INVITEE_PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mustChangePassword").value(false));
    }

    @Test
    void theInvitationTokenCannotBeUsedTwice() throws Exception {
        inviteStudent();
        String token = invitationTokenFor(INVITEE_EMAIL);

        acceptInvitation(token, INVITEE_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/accept-invitation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"An0ther!secret"}
                                """.formatted(token)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void anUnknownInvitationTokenIsRejected() throws Exception {
        inviteStudent();

        mockMvc.perform(post("/api/v1/auth/accept-invitation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"not-a-real-token","newPassword":"%s"}
                                """.formatted(INVITEE_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theAdministratorNeverSeesTheInvitationToken() throws Exception {
        String adminToken = login(ADMIN_EMAIL, ADMIN_PASSWORD).at("/data/tokens/accessToken").asText();

        String body = mockMvc.perform(post("/api/v1/invitations")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ada Lovelace","email":"%s","role":"STUDENT"}
                                """.formatted(INVITEE_EMAIL)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // Only the invitee's inbox carries the token; the API response must not,
        // or an admin could take over the account before it is claimed.
        assertThat(body).doesNotContain(invitationTokenFor(INVITEE_EMAIL));
        assertThat(mailSender.sent()).singleElement()
                .satisfies(message -> assertThat(message.getTo()).isEqualTo(INVITEE_EMAIL));
    }

    @Test
    void theInvitationEmailCarriesTheAcceptLinkAndNoPassword() throws Exception {
        inviteStudent();

        assertThat(mailSender.lastTo(INVITEE_EMAIL).getBody())
                .contains("/auth/accept-invitation?token=")
                .doesNotContain("Temporary password:");
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

    /** Claims the invitation with the emailed token, as an invitee would. */
    private void acceptInvitation(String token, String newPassword) throws Exception {
        mockMvc.perform(post("/api/v1/auth/accept-invitation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"%s"}
                                """.formatted(token, newPassword)))
                .andExpect(status().isOk());
    }

    /** The raw token exists only in the invitation email; the DB stores its hash. */
    private String invitationTokenFor(String email) {
        String body = mailSender.lastTo(email).getBody();
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("accept-invitation\\?token=(\\S+)").matcher(body);
        assertThat(matcher.find()).as("invitation token in the email").isTrue();
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
