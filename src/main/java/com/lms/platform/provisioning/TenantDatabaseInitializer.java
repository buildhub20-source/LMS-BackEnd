package com.lms.platform.provisioning;

import com.lms.platform.entity.Tenant;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Applies the full LMS schema and seeds the first tenant-local administrator. */
@Component
public class TenantDatabaseInitializer {
    private final PasswordEncoder passwordEncoder;

    public TenantDatabaseInitializer(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public void initialize(Tenant tenant, String databasePassword, String ownerPassword) {
        DataSource dataSource = dataSource(tenant.getJdbcUrl(), tenant.getDatabaseUsername(), databasePassword);
        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .schemas("lms")
                .defaultSchema("lms")
                .createSchemas(true)
                .baselineOnMigrate(true)
                .load()
                .migrate();
        seedTenantAdmin(dataSource, tenant, ownerPassword);
    }

    private void seedTenantAdmin(DataSource dataSource, Tenant tenant, String ownerPassword) {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbc.update("""
                INSERT INTO lms.users (id, name, email, password, is_active, is_locked, created_at, updated_at)
                VALUES (?, ?, ?, ?, TRUE, FALSE, ?, ?)
                ON CONFLICT (email) DO NOTHING
                """, userId, tenant.getOwnerName(), tenant.getOwnerEmail(), passwordEncoder.encode(ownerPassword),
                Timestamp.from(now), Timestamp.from(now));
        UUID persistedId = jdbc.queryForObject(
                "SELECT id FROM lms.users WHERE email = ?", UUID.class, tenant.getOwnerEmail());
        UUID adminRoleId = jdbc.queryForObject(
                "SELECT id FROM lms.roles WHERE name = 'ADMIN'", UUID.class);
        jdbc.update("""
                INSERT INTO lms.user_role (user_id, role_id, assigned_at)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id, role_id) DO NOTHING
                """, persistedId, adminRoleId, Timestamp.from(now));
    }

    private DataSource dataSource(String url, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }
}
