package com.lms.user.service;

import com.lms.common.audit.AuditAction;
import com.lms.common.audit.AuditService;
import com.lms.user.entity.AccountStatus;
import com.lms.user.entity.AccountStatusHistory;
import com.lms.user.entity.User;
import com.lms.user.repository.AccountStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountStatusServiceImpl implements AccountStatusService {

    private static final String RESOURCE = "USER";

    private final AccountStatusHistoryRepository historyRepository;
    private final AuditService auditService;

    @Override
    @Transactional
    public void activate(User user, UUID actorId, String reason) {
        if (user.isActive()) {
            return;
        }
        user.setActive(true);
        transition(user, AccountStatus.ACTIVE, actorId, reason, AuditAction.ACCOUNT_ACTIVATED);
    }

    @Override
    @Transactional
    public void deactivate(User user, UUID actorId, String reason) {
        if (!user.isActive()) {
            return;
        }
        user.setActive(false);
        transition(user, AccountStatus.INACTIVE, actorId, reason, AuditAction.ACCOUNT_DEACTIVATED);
    }

    @Override
    @Transactional
    public void lock(User user, UUID actorId, String reason) {
        if (user.isLocked()) {
            return;
        }
        user.setLocked(true);
        transition(user, AccountStatus.LOCKED, actorId, reason, AuditAction.ACCOUNT_LOCKED);
        log.warn("Account {} locked: {}", user.getEmail(), reason);
    }

    @Override
    @Transactional
    public void unlock(User user, UUID actorId, String reason) {
        if (!user.isLocked()) {
            return;
        }
        user.setLocked(false);
        transition(user, user.isActive() ? AccountStatus.ACTIVE : AccountStatus.INACTIVE,
                actorId, reason, AuditAction.ACCOUNT_UNLOCKED);
    }

    @Override
    @Transactional
    public void recordTransition(UUID userId, AccountStatus status, UUID actorId, String reason) {
        historyRepository.save(AccountStatusHistory.builder()
                .userId(userId)
                .status(status)
                .changedBy(actorId)
                .reason(reason)
                .build());
    }

    @Override
    public List<AccountStatusHistory> history(UUID userId) {
        return historyRepository.findAllByUserIdOrderByChangedAtDesc(userId);
    }

    private void transition(User user, AccountStatus status, UUID actorId, String reason, AuditAction action) {
        recordTransition(user.getId(), status, actorId, reason);
        auditService.record(actorId, action, RESOURCE, user.getId(), reason);
    }
}
