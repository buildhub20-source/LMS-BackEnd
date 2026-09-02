package com.lms.platform.repository;

import com.lms.platform.entity.TenantAuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantAuditEventRepository extends JpaRepository<TenantAuditEvent, UUID> {
}
