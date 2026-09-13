package com.lms.platform.repository;

import com.lms.platform.entity.TenantAuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TenantAuditEventRepository extends JpaRepository<TenantAuditEvent, UUID> {
    List<TenantAuditEvent> findAllByOrderByCreatedAtDesc();
    Page<TenantAuditEvent> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<TenantAuditEvent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    Page<TenantAuditEvent> findByTenantIdOrderByCreatedAtDesc(UUID tenantId, Pageable pageable);
}
