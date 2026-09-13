package com.lms.platform.service;

import com.lms.auth.service.SessionService;
import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.platform.dto.TenantImpersonationResponse;
import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantAuditEvent;
import com.lms.platform.entity.TenantStatus;
import com.lms.platform.repository.TenantAuditEventRepository;
import com.lms.platform.repository.TenantRepository;
import com.lms.platform.runtime.TenantConnection;
import com.lms.platform.runtime.TenantContext;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.security.jwt.JwtService;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TenantImpersonationService {

    private final TenantRepository tenantRepository;
    private final TenantAuditEventRepository auditRepository;
    private final TenantSecretCipher cipher;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public TenantImpersonationResponse impersonate(UUID tenantId, UUID platformAdminId) {
        return impersonate(tenantId, platformAdminId, "Bug Analysis & Diagnostics", 30);
    }

    public TenantImpersonationResponse impersonate(UUID tenantId, UUID platformAdminId, String reason, int durationMinutes) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ResourceNotFoundException.of("Tenant", tenantId));

        if (tenant.getStatus() != TenantStatus.ACTIVE) {
            throw new ApplicationException(ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Cannot open debug session for tenant in status " + tenant.getStatus());
        }

        int duration = (durationMinutes > 0 && durationMinutes <= 60) ? durationMinutes : 30;
        Duration ttl = Duration.ofMinutes(duration);
        Instant expiresAt = Instant.now().plus(ttl);
        String sessionReason = (reason != null && !reason.isBlank()) ? reason.trim() : "Bug Analysis & Diagnostics";

        String password = cipher.decrypt(tenant.getEncryptedDatabasePassword());
        TenantConnection connection = new TenantConnection(
                tenant.getId(),
                tenant.getSlug(),
                tenant.getJdbcUrl(),
                tenant.getDatabaseUsername(),
                password
        );

        // Ensure dedicated temporary Platform Support user exists in the tenant's database schema
        ensureSupportUser(tenant, password, platformAdminId);

        String accessToken;
        String userEmail;
        String userName;
        String roleName = "ADMIN";

        TenantContext.set(connection);
        try {
            String supportEmail = "support-debug@" + tenant.getSlug() + ".local";
            User user = userRepository.findByEmailWithAuthorities(supportEmail)
                    .orElseThrow(() -> new ApplicationException(ErrorCode.RESOURCE_NOT_FOUND,
                            "Support account could not be loaded in workspace"));

            userEmail = user.getEmail();
            userName = user.getName();

            SessionService.IssuedSession session = sessionService.openSession(user);
            LmsUserDetails principal = LmsUserDetails.from(user);
            accessToken = jwtService.generateAccessToken(principal, session.getSessionId(), ttl);

        } finally {
            TenantContext.clear();
        }

        // Save platform audit record on control plane
        auditRepository.save(TenantAuditEvent.builder()
                .tenantId(tenant.getId())
                .actorId(platformAdminId)
                .eventType("DEBUG_SESSION_STARTED")
                .message(String.format("Temporary %d-minute debug access opened for %s. Reason: %s",
                        duration, userEmail, sessionReason))
                .createdAt(Instant.now())
                .build());

        log.info("Platform Admin {} opened {}-min debug session on tenant '{}' as {}. Reason: {}",
                platformAdminId, duration, tenant.getSlug(), userEmail, sessionReason);

        return new TenantImpersonationResponse(
                accessToken,
                "Bearer",
                ttl.toSeconds(),
                expiresAt,
                tenant.getSlug(),
                tenant.getId(),
                userEmail,
                userName,
                roleName,
                "/admin/analytics",
                sessionReason
        );
    }

    private void ensureSupportUser(Tenant tenant, String password, UUID platformAdminId) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(tenant.getJdbcUrl());
        dataSource.setUsername(tenant.getDatabaseUsername());
        dataSource.setPassword(password);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Instant now = Instant.now();
        String supportEmail = "support-debug@" + tenant.getSlug() + ".local";
        String supportName = "Platform Support (Debug)";

        jdbc.update("""
                INSERT INTO lms.users (id, name, email, password, is_active, is_locked, created_at, updated_at)
                VALUES (?, ?, ?, ?, TRUE, FALSE, ?, ?)
                ON CONFLICT (email) DO UPDATE SET is_active = TRUE, is_locked = FALSE, updated_at = ?
                """, UUID.randomUUID(), supportName, supportEmail, passwordEncoder.encode(UUID.randomUUID().toString()),
                Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));

        UUID persistedId = jdbc.queryForObject(
                "SELECT id FROM lms.users WHERE email = ?", UUID.class, supportEmail);
        UUID adminRoleId = jdbc.queryForObject(
                "SELECT id FROM lms.roles WHERE name = 'ADMIN'", UUID.class);

        jdbc.update("""
                INSERT INTO lms.user_role (user_id, role_id, assigned_at)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id, role_id) DO NOTHING
                """, persistedId, adminRoleId, Timestamp.from(now));
    }
}
