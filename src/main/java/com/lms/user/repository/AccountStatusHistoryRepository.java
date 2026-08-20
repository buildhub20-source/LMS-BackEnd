package com.lms.user.repository;

import com.lms.user.entity.AccountStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AccountStatusHistoryRepository extends JpaRepository<AccountStatusHistory, UUID> {

    List<AccountStatusHistory> findAllByUserIdOrderByChangedAtDesc(UUID userId);

    Page<AccountStatusHistory> findAllByUserId(UUID userId, Pageable pageable);
}
