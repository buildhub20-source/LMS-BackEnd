package com.lms.platform.migration;

import com.lms.config.PlatformConfig;
import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantStatus;
import com.lms.platform.repository.TenantRepository;
import com.lms.platform.service.TenantSecretCipher;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Moves the control-plane role between two already-existing cloud projects.
 *
 * <p>The runner is opt-in, performs no deletes, and makes an environment-file
 * backup before it points this service at the new control plane. It is meant
 * for a controlled, one-time migration—not normal request handling.</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "lms.platform.role-migration", name = "enabled", havingValue = "true")
public class ControlPlaneRoleMigrationRunner {

    @Bean
    ApplicationRunner moveControlPlane(TenantRepository sourceTenantRepository,
                                       TenantSecretCipher cipher,
                                       PlatformConfig config,
                                       PasswordEncoder passwordEncoder,
                                       @Qualifier("controlPlaneDataSource") DataSource sourceDataSource) {
        return args -> {
            PlatformConfig.RoleMigration migration = config.getRoleMigration();
            if (migration.getSourceTenantSlug() == null || migration.getSourceTenantSlug().isBlank()) {
                throw new IllegalStateException("A source tenant slug is required for a control-plane role migration");
            }
            if (migration.getLegacyTenantSlug() == null || migration.getLegacyTenantSlug().isBlank()) {
                throw new IllegalStateException("A legacy tenant slug is required for a control-plane role migration");
            }
            if (!config.isEnabled() || !config.getGlobalAdmin().isEnabled()) {
                throw new IllegalStateException("Platform administration must be enabled for a control-plane role migration");
            }

            Tenant targetControlPlane = sourceTenantRepository.findBySlug(
                            migration.getSourceTenantSlug().trim().toLowerCase(Locale.ROOT))
                    .orElseThrow(() -> new IllegalStateException("The selected target control-plane tenant was not found"));
            if (targetControlPlane.getJdbcUrl() == null || targetControlPlane.getDatabaseUsername() == null
                    || targetControlPlane.getEncryptedDatabasePassword() == null) {
                throw new IllegalStateException("The selected target control-plane tenant has incomplete cloud credentials");
            }

            String targetPassword = cipher.decrypt(targetControlPlane.getEncryptedDatabasePassword());
            DataSource targetDataSource = cloudDataSource(targetControlPlane.getJdbcUrl(),
                    targetControlPlane.getDatabaseUsername(), targetPassword);

            // The target was previously a tenant and may already contain these
            // migrations. Flyway records that state and only applies what is missing.
            Flyway.configure()
                    .dataSource(targetDataSource)
                    .locations("classpath:db/migration")
                    .schemas("lms")
                    .defaultSchema("lms")
                    .createSchemas(true)
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();

            JdbcTemplate targetJdbc = new JdbcTemplate(targetDataSource);
            JdbcTemplate sourceJdbc = new JdbcTemplate(sourceDataSource);
            Timestamp now = Timestamp.from(Instant.now());
            String platformEmail = required(config.getGlobalAdmin().getEmail(), "PLATFORM_ADMIN_EMAIL").toLowerCase(Locale.ROOT);
            String platformPassword = required(config.getGlobalAdmin().getPassword(), "PLATFORM_ADMIN_PASSWORD");

            targetJdbc.update("""
                    INSERT INTO platform.platform_admins (id, name, email, password_hash, is_active, created_at, updated_at)
                    VALUES (?, ?, ?, ?, TRUE, ?, ?)
                    ON CONFLICT (email) DO UPDATE SET name = EXCLUDED.name,
                        password_hash = EXCLUDED.password_hash, is_active = TRUE, updated_at = EXCLUDED.updated_at
                    """, UUID.randomUUID(), config.getGlobalAdmin().getName(), platformEmail,
                    passwordEncoder.encode(platformPassword), now, now);

            TenantOwner owner = legacyOwner(sourceJdbc, config);
            String legacySlug = migration.getLegacyTenantSlug().trim().toLowerCase(Locale.ROOT);
            String sourceUrl = jdbcUrl(sourceDataSource);
            String sourceUsername = databaseUsername(sourceDataSource);
            String sourcePassword = databasePassword(sourceDataSource);
            UUID legacyTenantId = targetJdbc.query("""
                            INSERT INTO platform.tenants
                                (id, name, slug, status, provider, provider_project_ref, jdbc_url, database_username,
                                 encrypted_database_password, owner_name, owner_email, provisioned_at, created_at, updated_at)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT (slug) DO UPDATE SET
                                name = EXCLUDED.name, status = EXCLUDED.status, provider = EXCLUDED.provider,
                                provider_project_ref = EXCLUDED.provider_project_ref, jdbc_url = EXCLUDED.jdbc_url,
                                database_username = EXCLUDED.database_username,
                                encrypted_database_password = EXCLUDED.encrypted_database_password,
                                owner_name = EXCLUDED.owner_name, owner_email = EXCLUDED.owner_email,
                                provisioned_at = EXCLUDED.provisioned_at, updated_at = EXCLUDED.updated_at
                            RETURNING id
                            """,
                    (rs, rowNum) -> (UUID) rs.getObject(1), UUID.randomUUID(), "LMS", legacySlug,
                    TenantStatus.ACTIVE.name(), "SUPABASE", projectRef(sourceUsername), sourceUrl, sourceUsername,
                    cipher.encrypt(sourcePassword), owner.name(), owner.email(), now, now, now)
                    .stream().findFirst().orElseThrow(() -> new IllegalStateException("Unable to register the LMS tenant"));

            targetJdbc.update("""
                    INSERT INTO platform.tenant_audit_events (id, tenant_id, event_type, message, created_at)
                    VALUES (?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), legacyTenantId, "CONTROL_PLANE_ROLE_MIGRATED",
                    "Existing LMS cloud project registered as the legacy-data tenant during control-plane migration", now);

            backupAndSwitchEnvironment(targetControlPlane.getJdbcUrl(), targetControlPlane.getDatabaseUsername(), targetPassword);
        };
    }

    private TenantOwner legacyOwner(JdbcTemplate sourceJdbc, PlatformConfig config) {
        List<TenantOwner> owners = sourceJdbc.query("""
                        SELECT u.name, u.email
                        FROM lms.users u
                        JOIN lms.user_role ur ON ur.user_id = u.id
                        JOIN lms.roles r ON r.id = ur.role_id
                        WHERE u.is_active = TRUE AND r.name = 'ADMIN'
                        ORDER BY u.created_at ASC
                        LIMIT 1
                        """, (rs, rowNum) -> new TenantOwner(rs.getString("name"), rs.getString("email")));
        if (!owners.isEmpty()) return owners.get(0);
        return new TenantOwner(config.getGlobalAdmin().getName(), config.getGlobalAdmin().getEmail().toLowerCase(Locale.ROOT));
    }

    private void backupAndSwitchEnvironment(String url, String username, String password) throws IOException {
        Path env = Path.of(System.getProperty("user.dir"), ".env");
        if (!Files.exists(env)) throw new IllegalStateException("Backend .env was not found; refusing to switch configuration");
        Path backup = env.resolveSibling(".env.control-plane-before-role-migration.bak");
        if (!Files.exists(backup)) Files.copy(env, backup, StandardCopyOption.COPY_ATTRIBUTES);

        List<String> updated = new ArrayList<>();
        boolean urlSet = false;
        boolean usernameSet = false;
        boolean passwordSet = false;
        for (String line : Files.readAllLines(env)) {
            if (line.startsWith("DB_URL=")) {
                updated.add("DB_URL=" + url);
                urlSet = true;
            } else if (line.startsWith("DB_USERNAME=")) {
                updated.add("DB_USERNAME=" + username);
                usernameSet = true;
            } else if (line.startsWith("DB_PASSWORD=")) {
                updated.add("DB_PASSWORD=" + password);
                passwordSet = true;
            } else {
                updated.add(line);
            }
        }
        if (!urlSet || !usernameSet || !passwordSet) {
            throw new IllegalStateException("Backend .env does not contain all DB_URL, DB_USERNAME, and DB_PASSWORD settings");
        }
        Files.write(env, updated);
    }

    private DataSource cloudDataSource(String url, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

    private String jdbcUrl(DataSource dataSource) {
        if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hikari) return hikari.getJdbcUrl();
        throw new IllegalStateException("The control-plane data source must expose its JDBC URL");
    }

    private String databaseUsername(DataSource dataSource) {
        if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hikari) return hikari.getUsername();
        throw new IllegalStateException("The control-plane data source must expose its database username");
    }

    private String databasePassword(DataSource dataSource) {
        if (dataSource instanceof com.zaxxer.hikari.HikariDataSource hikari) return hikari.getPassword();
        throw new IllegalStateException("The control-plane data source must expose its database password");
    }

    private String projectRef(String databaseUsername) {
        String prefix = "postgres.";
        return databaseUsername != null && databaseUsername.startsWith(prefix)
                ? databaseUsername.substring(prefix.length()) : null;
    }

    private String required(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
        return value.trim();
    }

    private record TenantOwner(String name, String email) { }
}
