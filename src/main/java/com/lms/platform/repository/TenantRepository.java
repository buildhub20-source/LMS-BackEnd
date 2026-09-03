package com.lms.platform.repository;

import com.lms.platform.entity.Tenant;
import com.lms.platform.entity.TenantStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
    Optional<Tenant> findBySlug(String slug);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tenant from Tenant tenant where tenant.id = :tenantId")
    Optional<Tenant> findByIdForProvisioning(@Param("tenantId") UUID tenantId);

    List<Tenant> findByStatus(TenantStatus status);
    boolean existsBySlug(String slug);
}
