package com.lms.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(schema = "platform", name = "tenant_audit_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantAuditEvent {
    @Id @GeneratedValue private UUID id;
    @Column(name = "tenant_id", nullable = false) private UUID tenantId;
    @Column(name = "actor_id") private UUID actorId;
    @Column(name = "event_type", nullable = false, length = 64) private String eventType;
    @Column(length = 1000) private String message;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
}
