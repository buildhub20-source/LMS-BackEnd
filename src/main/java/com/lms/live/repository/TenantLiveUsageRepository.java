package com.lms.live.repository;

import com.lms.live.entity.TenantLiveUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantLiveUsageRepository extends JpaRepository<TenantLiveUsage, UUID> {

    Optional<TenantLiveUsage> findByTenantIdAndBillingMonth(UUID tenantId, String billingMonth);
}
