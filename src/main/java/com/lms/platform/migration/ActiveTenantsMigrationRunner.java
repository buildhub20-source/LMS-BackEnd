package com.lms.platform.migration;

import com.lms.config.PlatformConfig;
import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantStatus;
import com.lms.platform.repository.TenantRepository;
import com.lms.platform.service.TenantSecretCipher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * On application startup, applies any pending Flyway schema migrations to all
 * ACTIVE tenant databases so that tenant schemas are always kept up-to-date with
 * the control-plane codebase.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveTenantsMigrationRunner implements ApplicationRunner {

    private final PlatformConfig platformConfig;
    private final TenantRepository tenantRepository;
    private final TenantSecretCipher cipher;

    @Override
    public void run(ApplicationArguments args) {
        if (!platformConfig.isEnabled()) {
            return;
        }

        try {
            List<Tenant> activeTenants = tenantRepository.findByStatus(TenantStatus.ACTIVE);
            log.info("Checking schema migrations for {} active tenant(s)...", activeTenants.size());

            for (Tenant tenant : activeTenants) {
                try {
                    if (tenant.getJdbcUrl() == null || tenant.getEncryptedDatabasePassword() == null) {
                        continue;
                    }
                    String password = cipher.decrypt(tenant.getEncryptedDatabasePassword());
                    DriverManagerDataSource ds = new DriverManagerDataSource();
                    ds.setUrl(tenant.getJdbcUrl());
                    ds.setUsername(tenant.getDatabaseUsername());
                    ds.setPassword(password);

                    log.info("Applying Flyway migrations for tenant '{}' ({})", tenant.getName(), tenant.getSlug());
                    Flyway.configure()
                            .dataSource(ds)
                            .locations("classpath:db/migration")
                            .schemas("lms")
                            .defaultSchema("lms")
                            .createSchemas(true)
                            .baselineOnMigrate(true)
                            .outOfOrder(true)
                            .validateOnMigrate(false)
                            .load()
                            .migrate();
                    log.info("Flyway migrations successfully applied for tenant '{}'", tenant.getSlug());
                } catch (Exception e) {
                    log.error("Failed to migrate database for tenant '{}': {}", tenant.getSlug(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error during active tenants migration runner: {}", e.getMessage(), e);
        }
    }
}
