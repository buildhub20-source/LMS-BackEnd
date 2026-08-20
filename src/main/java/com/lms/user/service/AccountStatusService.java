package com.lms.user.service;

import com.lms.user.entity.AccountStatus;
import com.lms.user.entity.AccountStatusHistory;
import com.lms.user.entity.User;

import java.util.List;
import java.util.UUID;

/**
 * Owns every account status transition.
 *
 * <p>Centralised so that flipping {@code is_active} or {@code is_locked} always
 * writes the matching {@code account_status_history} and {@code audit_log} rows.
 * Nothing else in the codebase should set those flags directly.
 */
public interface AccountStatusService {

    void activate(User user, UUID actorId, String reason);

    void deactivate(User user, UUID actorId, String reason);

    void lock(User user, UUID actorId, String reason);

    void unlock(User user, UUID actorId, String reason);

    /** Records a transition without changing the account, used at creation time. */
    void recordTransition(UUID userId, AccountStatus status, UUID actorId, String reason);

    List<AccountStatusHistory> history(UUID userId);
}
