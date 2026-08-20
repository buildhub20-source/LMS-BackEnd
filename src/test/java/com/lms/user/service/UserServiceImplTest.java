package com.lms.user.service;

import com.lms.auth.service.SessionService;
import com.lms.common.audit.AuditAction;
import com.lms.common.audit.AuditService;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.role.entity.Role;
import com.lms.role.service.RoleService;
import com.lms.user.dto.request.ChangePasswordRequest;
import com.lms.user.dto.request.UpdateUserRequest;
import com.lms.user.dto.request.UpdateUserRolesRequest;
import com.lms.user.entity.User;
import com.lms.user.event.PasswordChangedEvent;
import com.lms.user.mapper.UserMapper;
import com.lms.user.mapper.UserMapperImpl;
import com.lms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private AccountStatusService accountStatusService;

    @Mock
    private SessionService sessionService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private UserMapper userMapper = new UserMapperImpl();

    @InjectMocks
    private UserServiceImpl userService;

    private Role studentRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        studentRole = role("STUDENT");
        adminRole = role("ADMIN");
    }

    @Test
    void updateChangesOnlyTheFieldsThatWereSupplied() {
        User user = activeUser("grace@lms.test", "Grace Hopper");
        user.setPhone("+1 555 0100");
        when(userRepository.findByIdWithAuthorities(user.getId())).thenReturn(Optional.of(user));

        var response = userService.update(user.getId(), new UpdateUserRequest("Grace B. Hopper", null, null));

        assertThat(response.getName()).isEqualTo("Grace B. Hopper");
        assertThat(response.getPhone()).isEqualTo("+1 555 0100");
    }

    @Test
    void changePasswordRequiresTheCurrentPassword() {
        User user = activeUser("grace@lms.test", "Grace Hopper");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "stored")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(user.getId(),
                new ChangePasswordRequest("wrong", "N3w!password")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Current password");

        verify(sessionService, never()).revokeAllForUser(any());
    }

    @Test
    void changePasswordRejectsReusingTheSameValue() {
        User user = activeUser("grace@lms.test", "Grace Hopper");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(user.getId(),
                new ChangePasswordRequest("same", "S4me!password")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("differ");
    }

    @Test
    void changePasswordEndsEveryExistingSession() {
        User user = activeUser("grace@lms.test", "Grace Hopper");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("current", "stored")).thenReturn(true);
        when(passwordEncoder.matches("N3w!password", "stored")).thenReturn(false);
        when(passwordEncoder.encode("N3w!password")).thenReturn("new-hash");

        userService.changePassword(user.getId(), new ChangePasswordRequest("current", "N3w!password"));

        assertThat(user.getPassword()).isEqualTo("new-hash");
        verify(sessionService).revokeAllForUser(user.getId());
        // Signals the invitation module to close out onboarding.
        verify(eventPublisher).publishEvent(new PasswordChangedEvent(user.getId()));
        verify(auditService).record(isNull(), eq(AuditAction.PASSWORD_CHANGED), eq("USER"),
                eq(user.getId()), isNull());
    }

    @Test
    void changePasswordIsRefusedForAnAccountThatWasNeverActivated() {
        User user = activeUser("pending@lms.test", "Pending Person");
        user.setPassword(null);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.changePassword(user.getId(),
                new ChangePasswordRequest("anything", "N3w!password")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("accept the invitation");
    }

    @Test
    void updateRolesRecordsWhatWasAddedAndRemoved() {
        User user = activeUser("grace@lms.test", "Grace Hopper");
        user.assignRole(studentRole, null);

        when(userRepository.findByIdWithAuthorities(user.getId())).thenReturn(Optional.of(user));
        when(roleService.resolveByNames(Set.of("ADMIN"))).thenReturn(Set.of(adminRole));

        var response = userService.updateRoles(user.getId(), new UpdateUserRolesRequest(Set.of("ADMIN")));

        assertThat(response.getRoles()).containsExactly("ADMIN");
        verify(auditService).record(isNull(), eq(AuditAction.ROLE_ASSIGNED), eq("USER"),
                eq(user.getId()), eq("ADMIN"));
        verify(auditService).record(isNull(), eq(AuditAction.ROLE_REMOVED), eq("USER"),
                eq(user.getId()), eq("STUDENT"));
    }

    @Test
    void theLastAdministratorCannotLoseTheAdminRole() {
        User user = activeUser("admin@lms.test", "Platform Administrator");
        user.assignRole(adminRole, null);

        when(userRepository.findByIdWithAuthorities(user.getId())).thenReturn(Optional.of(user));
        when(roleService.resolveByNames(Set.of("STUDENT"))).thenReturn(Set.of(studentRole));
        when(userRepository.countByRoleName("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateRoles(user.getId(),
                new UpdateUserRolesRequest(Set.of("STUDENT"))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("last administrator");
    }

    @Test
    void theLastAdministratorCannotBeLocked() {
        User user = activeUser("admin@lms.test", "Platform Administrator");
        user.assignRole(adminRole, null);

        when(userRepository.findByIdWithAuthorities(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.countByRoleName("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> userService.lock(user.getId(), "testing"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("last administrator");

        verify(accountStatusService, never()).lock(any(), any(), anyString());
    }

    @Test
    void deactivatingEndsEverySessionForThatUser() {
        User user = activeUser("grace@lms.test", "Grace Hopper");
        user.assignRole(studentRole, null);
        when(userRepository.findByIdWithAuthorities(user.getId())).thenReturn(Optional.of(user));

        userService.deactivate(user.getId(), "left the organisation");

        verify(accountStatusService).deactivate(user, null, "left the organisation");
        verify(sessionService).revokeAllForUser(user.getId());
    }

    @Test
    void lookupsFailForAnUnknownUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.findByIdWithAuthorities(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deactivate(id, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private Role role(String name) {
        return Role.builder().id(UUID.randomUUID()).name(name).permissions(new HashSet<>()).build();
    }

    private User activeUser(String email, String name) {
        return User.builder()
                .id(UUID.randomUUID())
                .name(name)
                .email(email)
                .password("stored")
                .active(true)
                .locked(false)
                .userRoles(new HashSet<>())
                .build();
    }
}
