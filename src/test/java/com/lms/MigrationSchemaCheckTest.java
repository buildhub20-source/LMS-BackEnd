package com.lms;

import com.lms.assessment.repository.AssessmentRepository;
import com.lms.permission.repository.PermissionRepository;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import com.lms.role.entity.Role;
import com.lms.role.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Runs the real Flyway migrations and then lets Hibernate validate the entity
 * model against the resulting schema. A column renamed in an entity but not in
 * a migration (or the reverse) fails here rather than on deployment.
 *
 * <p>The migrations use standard SQL so they run on both PostgreSQL and the H2
 * PostgreSQL compatibility mode used here. Anything genuinely PostgreSQL
 * specific still needs a check against a real database.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.datasource.url=jdbc:h2:mem:migrationcheck;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;INIT=CREATE SCHEMA IF NOT EXISTS lms"
})
class MigrationSchemaCheckTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AssessmentRepository assessmentRepository;

    @Test
    void migrationsProduceASchemaTheEntityModelValidatesAgainst() {
        // Reaching this point means Flyway ran and ddl-auto=validate passed.
        assertThat(permissionRepository.count()).isPositive();
    }

    @Test
    void assessmentTablesAreCreatedAndValidatedByHibernate() {
        // If the assessment migration ran and the entity model matches, this succeeds.
        assertThat(assessmentRepository.count()).isZero();
    }

    @Test
    void assessmentPermissionsAreSeeded() {
        assertThat(permissionRepository.findAll())
                .extracting("name")
                .contains("ASSESSMENT_VIEW", "ASSESSMENT_CREATE",
                           "ASSESSMENT_UPDATE", "ASSESSMENT_DELETE", "ASSESSMENT_PUBLISH");
    }

    @Test
    void baselineRolesAreSeededWithTheirPermissions() {
        assertThat(roleRepository.findByName("ADMIN")).isPresent();
        assertThat(roleRepository.findByName("INSTRUCTOR")).isPresent();
        assertThat(roleRepository.findByName("STUDENT")).isPresent();

        Role admin = roleRepository.findByNameWithPermissions("ADMIN").orElseThrow();
        // ADMIN should hold every permission seeded across all migrations.
        assertThat(admin.getPermissions()).hasSameSizeAs(permissionRepository.findAll());

        Role student = roleRepository.findByNameWithPermissions("STUDENT").orElseThrow();
        // V6 grants COURSE_VIEW; V8 adds ASSESSMENT_VIEW.
        assertThat(student.getPermissions())
                .extracting("name")
                .containsExactlyInAnyOrder("COURSE_VIEW", "ASSESSMENT_VIEW");
    }

    @Test
    void theDatabaseRefusesAnEmailThatIsNotLowercase() {
        // The service layer lowercases every address, but the constraint is what
        // stops a direct write creating a second account that differs only by
        // case and would then be invisible to lower(email) lookups.
        assertThatThrownBy(() -> userRepository.saveAndFlush(user("Mixed.Case@lms.test")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseAcceptsALowercaseEmail() {
        assertThatCode(() -> userRepository.saveAndFlush(user("lower.case@lms.test")))
                .doesNotThrowAnyException();
    }

    private User user(String email) {
        return User.builder()
                .name("Schema Probe")
                .email(email)
                .password("irrelevant")
                .active(false)
                .locked(false)
                .build();
    }
}
