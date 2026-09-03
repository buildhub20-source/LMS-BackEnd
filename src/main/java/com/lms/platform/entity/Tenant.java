package com.lms.platform.entity;

import com.lms.common.audit.Timestamped;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Control-plane record for a tenant database. Secrets are encrypted at rest
 * and must never be returned from an API response or log entry.
 */
@Entity
@Table(schema = "platform", name = "tenants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant extends Timestamped {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(nullable = false, unique = true, length = 63)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TenantStatus status;

    @Column(nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_project_ref", length = 128)
    private String providerProjectRef;

    @Column(name = "jdbc_url", length = 1024)
    private String jdbcUrl;

    @Column(name = "database_username", length = 255)
    private String databaseUsername;

    @Column(name = "encrypted_database_password", columnDefinition = "TEXT")
    private String encryptedDatabasePassword;

    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @Column(name = "owner_email", nullable = false, length = 255)
    private String ownerEmail;

    @Column(name = "encrypted_owner_password", columnDefinition = "TEXT")
    private String encryptedOwnerPassword;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "deletion_scheduled_at")
    private Instant deletionScheduledAt;

    @Column(name = "provisioned_at")
    private Instant provisionedAt;
}
