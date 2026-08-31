package com.lms.common.audit;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.common.response.PageResponse;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Tag(name = "Audit Logs")
@RestController
@RequestMapping(ApiPaths.ADMIN_AUDIT_LOGS)
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Operation(summary = "Query audit log entries")
    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_VIEW') or hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String resource,
            @RequestParam(required = false) AuditAction action,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (resource != null && !resource.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("resource")), resource.trim().toLowerCase()));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<AuditLog> page = auditLogRepository.findAll(spec, pageable);

        Set<UUID> userIds = page.getContent().stream()
                .map(AuditLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, User> userMap = userIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        PageResponse<AuditLogResponse> response = PageResponse.from(page, log -> {
            User user = log.getUserId() != null ? userMap.get(log.getUserId()) : null;
            return AuditLogResponse.builder()
                    .id(log.getId())
                    .userId(log.getUserId())
                    .userName(user != null ? user.getName() : null)
                    .userEmail(user != null ? user.getEmail() : null)
                    .action(log.getAction())
                    .resource(log.getResource())
                    .resourceId(log.getResourceId())
                    .details(log.getDetails())
                    .ipAddress(log.getIpAddress())
                    .userAgent(log.getUserAgent())
                    .createdAt(log.getCreatedAt())
                    .build();
        });

        return ResponseEntity.ok(ApiResponse.of(response));
    }
}
