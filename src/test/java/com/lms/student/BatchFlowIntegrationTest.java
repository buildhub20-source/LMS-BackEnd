package com.lms.student;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.permission.entity.Permission;
import com.lms.permission.repository.PermissionRepository;
import com.lms.role.entity.Role;
import com.lms.role.repository.RoleRepository;
import com.lms.student.dto.request.CreateBatchRequest;
import com.lms.student.dto.request.UpdateBatchRequest;
import com.lms.student.entity.BatchStatus;
import com.lms.student.entity.DeliveryMode;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
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

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BatchFlowIntegrationTest {

    private static final String ADMIN_EMAIL = "batchadmin@lms.test";
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
    private PasswordEncoder passwordEncoder;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        Permission p1 = permissionRepository.findByName("BATCH_VIEW")
                .orElseGet(() -> permissionRepository.saveAndFlush(Permission.builder().name("BATCH_VIEW").resource("BATCH").action("VIEW").description("View batches").build()));
        Permission p2 = permissionRepository.findByName("BATCH_CREATE")
                .orElseGet(() -> permissionRepository.saveAndFlush(Permission.builder().name("BATCH_CREATE").resource("BATCH").action("CREATE").description("Create batch").build()));
        Permission p3 = permissionRepository.findByName("BATCH_UPDATE")
                .orElseGet(() -> permissionRepository.saveAndFlush(Permission.builder().name("BATCH_UPDATE").resource("BATCH").action("UPDATE").description("Update batch").build()));
        Permission p4 = permissionRepository.findByName("BATCH_DELETE")
                .orElseGet(() -> permissionRepository.saveAndFlush(Permission.builder().name("BATCH_DELETE").resource("BATCH").action("DELETE").description("Delete batch").build()));

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseGet(() -> roleRepository.saveAndFlush(Role.builder().name("ADMIN").description("Admin role").build()));
        Set<Permission> perms = new HashSet<>(adminRole.getPermissions() != null ? adminRole.getPermissions() : Set.of());
        perms.add(p1);
        perms.add(p2);
        perms.add(p3);
        perms.add(p4);
        adminRole.setPermissions(perms);
        roleRepository.saveAndFlush(adminRole);

        User admin = userRepository.findByEmailIgnoreCase(ADMIN_EMAIL).orElseGet(() -> {
            User u = new User();
            u.setEmail(ADMIN_EMAIL);
            u.setPassword(passwordEncoder.encode(PASSWORD));
            u.setName("Batch Admin");
            u.setActive(true);
            u.assignRole(adminRole, null);
            return userRepository.saveAndFlush(u);
        });

        String loginPayload = objectMapper.writeValueAsString(new LoginReq(ADMIN_EMAIL, PASSWORD));
        MvcResult res = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        adminToken = json.at("/data/tokens/accessToken").asText();
    }

    @Test
    void testBatchCrudLifecycle() throws Exception {
        // 1. Search batches (should return ok)
        mockMvc.perform(get("/api/v1/batches")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        // 2. Create batch
        CreateBatchRequest createReq = new CreateBatchRequest();
        createReq.setCode("TEST-BATCH-01");
        createReq.setName("Test Batch Name");
        createReq.setStartDate(LocalDate.now());
        createReq.setEndDate(LocalDate.now().plusDays(30));
        createReq.setDeliveryMode(DeliveryMode.ONLINE);
        createReq.setCapacity(25);
        createReq.setStatus(BatchStatus.PLANNED);

        MvcResult createRes = mockMvc.perform(post("/api/v1/batches")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("TEST-BATCH-01"))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createRes.getResponse().getContentAsString());
        String batchIdStr = createdJson.path("data").path("id").asText();
        UUID batchId = UUID.fromString(batchIdStr);

        // 3. Get batch by ID
        mockMvc.perform(get("/api/v1/batches/" + batchId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Test Batch Name"));

        // 4. Update batch
        UpdateBatchRequest updateReq = new UpdateBatchRequest();
        updateReq.setName("Updated Batch Name");
        updateReq.setStatus(BatchStatus.ONGOING);

        mockMvc.perform(patch("/api/v1/batches/" + batchId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated Batch Name"))
                .andExpect(jsonPath("$.data.status").value("ONGOING"));

        // 5. Delete batch
        mockMvc.perform(delete("/api/v1/batches/" + batchId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // 6. Verify deleted
        mockMvc.perform(get("/api/v1/batches/" + batchId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());
    }

    private record LoginReq(String email, String password) {}
}
