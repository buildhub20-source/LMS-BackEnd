package com.lms.common.audit;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.common.response.PageResponse;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuditController auditController;

    private AuditLog sampleLog;
    private User sampleUser;
    private UUID sampleUserId;

    @BeforeEach
    void setUp() {
        sampleUserId = UUID.randomUUID();
        sampleUser = User.builder()
                .id(sampleUserId)
                .name("Admin User")
                .email("admin@lms.local")
                .build();

        sampleLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(sampleUserId)
                .action(AuditAction.LOGIN_SUCCESS)
                .resource("user")
                .resourceId(sampleUserId)
                .details("User signed in")
                .ipAddress("127.0.0.1")
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void getAuditLogsReturnsPageResponseWithActorDetails() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> page = new PageImpl<>(List.of(sampleLog), pageable, 1);
        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(List.of(sampleUser));

        ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> response =
                auditController.getAuditLogs(null, null, null, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        PageResponse<AuditLogResponse> pageResponse = response.getBody().getData();
        assertThat(pageResponse.getContent()).hasSize(1);
        assertThat(pageResponse.getTotalElements()).isEqualTo(1);
        assertThat(pageResponse.getTotalPages()).isEqualTo(1);
        assertThat(pageResponse.getContent().get(0).getAction()).isEqualTo(AuditAction.LOGIN_SUCCESS);
        assertThat(pageResponse.getContent().get(0).getUserName()).isEqualTo("Admin User");
        assertThat(pageResponse.getContent().get(0).getUserEmail()).isEqualTo("admin@lms.local");
    }

    @Test
    void getAuditLogsWithMultipleFiltersQueriesSpecification() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);
        Page<AuditLog> page = new PageImpl<>(List.of(sampleLog), pageable, 1);
        when(auditLogRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(userRepository.findAllById(any())).thenReturn(List.of(sampleUser));

        ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> response =
                auditController.getAuditLogs(userId, "USER", AuditAction.LOGIN_SUCCESS, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        ArgumentCaptor<Specification<AuditLog>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(auditLogRepository).findAll(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue()).isNotNull();
    }
}
