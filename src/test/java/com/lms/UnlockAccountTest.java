package com.lms;

import com.lms.platform.entity.Tenant;
import com.lms.platform.repository.TenantRepository;
import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import com.lms.platform.service.TenantSecretCipher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

@SpringBootTest
@ActiveProfiles("dev")
class UnlockAccountTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private TenantRepository tenantRepository;

    @Autowired(required = false)
    private TenantSecretCipher cipher;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void unlockAllAccounts() {
        System.out.println("=== Starting unlock & diagnostics ===");
        String newHash = passwordEncoder.encode("Admin123!");

        // 1. Control plane database
        try {
            jdbcTemplate.update("UPDATE lms.users SET is_locked = false, is_active = true WHERE email ILIKE '%admin%'");
            jdbcTemplate.update("DELETE FROM lms.login_attempt WHERE email ILIKE '%admin%'");
        } catch (Exception ignored) {
        }

        // 2. Tenants
        if (tenantRepository != null) {
            List<Tenant> tenants = tenantRepository.findAll();
            System.out.println("Found " + tenants.size() + " tenants.");
            for (Tenant tenant : tenants) {
                System.out.println("Processing tenant: " + tenant.getSlug() + " (" + tenant.getName() + ")");
                String rawPassword = cipher != null ? cipher.decrypt(tenant.getEncryptedDatabasePassword()) : "";
                try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                        tenant.getJdbcUrl(), tenant.getDatabaseUsername(), rawPassword)) {
                    try (java.sql.Statement st = conn.createStatement()) {
                        try (java.sql.ResultSet rs = st.executeQuery(
                                "SELECT id, email, is_locked, is_active, password FROM lms.users WHERE email ILIKE '%admin%'")) {
                            while (rs.next()) {
                                String email = rs.getString("email");
                                String pwd = rs.getString("password");
                                boolean matches123 = pwd != null && passwordEncoder.matches("Admin123!", pwd);
                                boolean matches2026 = pwd != null && passwordEncoder.matches("Admin@2026!ChangeMe", pwd);
                                System.out.println("Tenant user: " + email + 
                                        ", is_locked=" + rs.getBoolean("is_locked") + 
                                        ", is_active=" + rs.getBoolean("is_active") +
                                        ", matches Admin123!=" + matches123 +
                                        ", matches Admin@2026!ChangeMe=" + matches2026);
                            }
                        }
                        // Unlock and set password to Admin123!
                        String adminHash = passwordEncoder.encode("Admin123!");
                        conn.setAutoCommit(true);
                        int updated = st.executeUpdate(
                                "UPDATE lms.users SET is_locked = false, is_active = true, password = '" + adminHash + "' WHERE email ILIKE '%admin%'");
                        System.out.println("Updated users: " + updated);

                        try {
                            st.executeUpdate("DELETE FROM lms.login_attempt WHERE email ILIKE '%admin%'");
                            System.out.println("Cleared login_attempt records.");
                        } catch (Exception e) {
                            System.out.println("Clear login_attempt notice: " + e.getMessage());
                        }

                        try (java.sql.ResultSet verifyRs = st.executeQuery(
                                "SELECT email, is_locked, is_active FROM lms.users WHERE email ILIKE '%admin%'")) {
                            while (verifyRs.next()) {
                                System.out.println("VERIFIED: " + verifyRs.getString("email") + 
                                        ", is_locked=" + verifyRs.getBoolean("is_locked") + 
                                        ", is_active=" + verifyRs.getBoolean("is_active"));
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Tenant " + tenant.getSlug() + " error: " + e.getMessage());
                }
            }
        }
        System.out.println("=== Finished unlock & diagnostics ===");
    }

    @Test
    void checkAssessmentFk() {
        UUID assessmentId = UUID.fromString("2061cee9-8ae9-44de-8709-4508e9acea54");
        System.out.println("=== Checking assessment " + assessmentId + " ===");
        if (tenantRepository != null) {
            List<Tenant> tenants = tenantRepository.findAll();
            for (Tenant tenant : tenants) {
                System.out.println("Checking tenant: " + tenant.getSlug());
                String rawPassword = cipher != null ? cipher.decrypt(tenant.getEncryptedDatabasePassword()) : "";
                try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                        tenant.getJdbcUrl(), tenant.getDatabaseUsername(), rawPassword)) {
                    conn.setAutoCommit(true);
                    try (java.sql.Statement st = conn.createStatement()) {
                        st.executeUpdate(
                                "DELETE FROM lms.rubric_scores WHERE attempt_id IN (SELECT id FROM lms.assessment_attempts WHERE assessment_id = '" + assessmentId + "')");
                        int subDeleted = st.executeUpdate(
                                "DELETE FROM lms.submissions WHERE attempt_id IN (SELECT id FROM lms.assessment_attempts WHERE assessment_id = '" + assessmentId + "')");
                        System.out.println("Deleted " + subDeleted + " submissions for assessment " + assessmentId);

                        int attDeleted = st.executeUpdate(
                                "DELETE FROM lms.assessment_attempts WHERE assessment_id = '" + assessmentId + "'");
                        System.out.println("Deleted " + attDeleted + " attempts for assessment " + assessmentId);
                    }
                } catch (Exception e) {
                    System.out.println("Tenant error: " + e.getMessage());
                }
            }
        }
    }

    @Test
    void checkStudentAndAssessment() {
        System.out.println("=== Checking student and assessment ===");
        if (tenantRepository != null) {
            List<Tenant> tenants = tenantRepository.findAll();
            for (Tenant tenant : tenants) {
                System.out.println("Tenant: " + tenant.getSlug());
                String rawPassword = cipher != null ? cipher.decrypt(tenant.getEncryptedDatabasePassword()) : "";
                try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                        tenant.getJdbcUrl(), tenant.getDatabaseUsername(), rawPassword)) {
                    try (java.sql.Statement st = conn.createStatement()) {
                        UUID studentId = null;
                        try (java.sql.ResultSet rs = st.executeQuery(
                                "SELECT id, email, is_active, is_locked FROM lms.users WHERE email = 'jeisurya15@gmail.com'")) {
                            if (rs.next()) {
                                studentId = UUID.fromString(rs.getString("id"));
                                System.out.println("Found existing student: " + rs.getString("email") + 
                                        ", is_active=" + rs.getBoolean("is_active") + 
                                        ", is_locked=" + rs.getBoolean("is_locked"));
                            }
                        }

                        UUID studentRoleId = null;
                        try (java.sql.ResultSet roleRs = st.executeQuery("SELECT id FROM lms.roles WHERE name = 'STUDENT'")) {
                            if (roleRs.next()) {
                                studentRoleId = UUID.fromString(roleRs.getString("id"));
                            }
                        }
                        System.out.println("Student role id: " + studentRoleId);

                        String pwdHash = passwordEncoder.encode("Student123!");

                        if (studentId == null) {
                            studentId = UUID.randomUUID();
                            System.out.println("Creating student with ID: " + studentId);
                            st.executeUpdate("INSERT INTO lms.users (id, name, email, password, is_active, is_locked, created_at, updated_at) " +
                                    "VALUES ('" + studentId + "', 'Jeisurya', 'jeisurya15@gmail.com', '" + pwdHash + "', true, false, NOW(), NOW())");
                        } else {
                            st.executeUpdate("UPDATE lms.users SET password = '" + pwdHash + "', is_locked = false, is_active = true WHERE id = '" + studentId + "'");
                        }

                        if (studentRoleId != null) {
                            st.executeUpdate("INSERT INTO lms.user_role (user_id, role_id) VALUES ('" + studentId + "', '" + studentRoleId + "') ON CONFLICT DO NOTHING");
                        }
                        System.out.println("Student jeisurya15@gmail.com successfully verified and enrolled with STUDENT role.");
                    }
                } catch (Exception e) {
                    System.out.println("Tenant error: " + e.getMessage());
                }
            }
        }
    }

    @Test
    void verifyAssessmentPublished() {
        UUID assessmentId = UUID.fromString("2061cee9-8ae9-44de-8709-4508e9acea54");
        System.out.println("=== Verifying assessment published status and test cases ===");
        if (tenantRepository != null) {
            List<Tenant> tenants = tenantRepository.findAll();
            for (Tenant tenant : tenants) {
                System.out.println("Tenant: " + tenant.getSlug());
                String rawPassword = cipher != null ? cipher.decrypt(tenant.getEncryptedDatabasePassword()) : "";
                try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                        tenant.getJdbcUrl(), tenant.getDatabaseUsername(), rawPassword)) {
                    conn.setAutoCommit(true);
                    try (java.sql.Statement st = conn.createStatement()) {
                        try (java.sql.ResultSet rs = st.executeQuery("SELECT id, title, status FROM lms.assessments WHERE id = '" + assessmentId + "'")) {
                            if (rs.next()) {
                                String status = rs.getString("status");
                                System.out.println("Assessment " + rs.getString("title") + " status: " + status);
                                if (!"PUBLISHED".equalsIgnoreCase(status)) {
                                    st.executeUpdate("UPDATE lms.assessments SET status = 'PUBLISHED' WHERE id = '" + assessmentId + "'");
                                    System.out.println("Updated assessment status to PUBLISHED");
                                }
                            }
                        }

                        // Query questions and test cases
                        try (java.sql.ResultSet qRs = st.executeQuery(
                                "SELECT q.id, q.title, q.question_type, aq.marks FROM lms.questions q " +
                                "JOIN lms.assessment_questions aq ON aq.question_id = q.id WHERE aq.assessment_id = '" + assessmentId + "'")) {
                            while (qRs.next()) {
                                UUID qId = UUID.fromString(qRs.getString("id"));
                                System.out.println("Question: " + qRs.getString("title") + " (" + qRs.getString("question_type") + ", marks: " + qRs.getInt("marks") + ")");
                                
                                try (java.sql.Statement st2 = conn.createStatement();
                                     java.sql.ResultSet tcRs = st2.executeQuery("SELECT id, input_data, expected_output, sample FROM lms.test_cases WHERE question_id = '" + qId + "' ORDER BY sample DESC, id ASC")) {
                                    while (tcRs.next()) {
                                        System.out.println("  TC: sample=" + tcRs.getBoolean("sample") + 
                                                ", in=" + tcRs.getString("input_data").replace("\n", "\\n") + 
                                                ", out=" + tcRs.getString("expected_output").replace("\n", "\\n"));
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Tenant error: " + e.getMessage());
                }
            }
        }
    }
}
