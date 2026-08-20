package com.lms.common.audit;

import com.lms.common.util.HttpRequests;
import com.lms.security.authentication.AuthenticationService;
import com.lms.security.authentication.LmsUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void record(AuditAction action, String resource, UUID resourceId, String details) {
        UUID actorId = AuthenticationService.currentPrincipal()
                .map(LmsUserDetails::getUserId)
                .orElse(null);
        write(actorId, action, resource, resourceId, details);
    }

    @Override
    @Transactional
    public void record(UUID actorId, AuditAction action, String resource, UUID resourceId, String details) {
        write(actorId, action, resource, resourceId, details);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordIndependently(UUID actorId, AuditAction action, String resource,
                                    UUID resourceId, String details) {
        write(actorId, action, resource, resourceId, details);
    }

    private void write(UUID actorId, AuditAction action, String resource, UUID resourceId, String details) {
        AuditLog entry = AuditLog.builder()
                .userId(actorId)
                .action(action)
                .resource(resource)
                .resourceId(resourceId)
                .details(details)
                .ipAddress(HttpRequests.clientIp())
                .userAgent(HttpRequests.userAgent())
                .build();

        auditLogRepository.save(entry);
        log.debug("Audit {} on {} by {}", action, resource, actorId);
    }
}
