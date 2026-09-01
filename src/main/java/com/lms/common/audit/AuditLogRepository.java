package com.lms.common.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findAllByUserId(UUID userId, Pageable pageable);

    Page<AuditLog> findAllByResource(String resource, Pageable pageable);

    Page<AuditLog> findAllByAction(AuditAction action, Pageable pageable);
}
