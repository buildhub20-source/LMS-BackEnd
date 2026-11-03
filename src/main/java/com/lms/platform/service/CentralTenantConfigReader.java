package com.lms.platform.service;

import com.lms.platform.entity.TenantConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.util.UUID;

@Service
public class CentralTenantConfigReader {
    private final JdbcTemplate jdbc;
    public CentralTenantConfigReader(@Qualifier("controlPlaneDataSource") DataSource dataSource) {
        this.jdbc = new JdbcTemplate(dataSource);
    }
    public TenantConfig find(UUID tenantId) {
        return jdbc.query("SELECT * FROM platform.tenant_configs WHERE tenant_id = ?",
                new BeanPropertyRowMapper<>(TenantConfig.class), tenantId).stream().findFirst().orElse(null);
    }
    public void connectGoogle(UUID tenantId, String encryptedToken, String email) {
        if (jdbc.update("UPDATE platform.tenant_configs SET google_calendar_refresh_token = ?, google_calendar_email = ?, updated_at = CURRENT_TIMESTAMP WHERE tenant_id = ?",
                encryptedToken, email, tenantId) != 1) throw new IllegalStateException("Tenant configuration not found");
    }
}
