package com.lms.user.service;

import com.lms.auth.service.SessionService;
import com.lms.common.audit.AuditAction;
import com.lms.common.audit.AuditService;
import com.lms.common.exception.BusinessRuleException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.common.response.PageResponse;
import com.lms.common.util.PageRequests;
import com.lms.role.constants.SystemRoles;
import com.lms.role.entity.Role;
import com.lms.role.service.RoleService;
import com.lms.security.authentication.AuthenticationService;
import com.lms.security.authentication.LmsUserDetails;
import com.lms.invitation.repository.InvitationRepository;
import com.lms.user.dto.request.ChangePasswordRequest;
import com.lms.user.dto.request.UpdateUserRequest;
import com.lms.user.dto.request.UpdateUserRolesRequest;
import com.lms.user.dto.response.AccountStatusHistoryResponse;
import com.lms.user.dto.response.UserResponse;
import com.lms.user.entity.User;
import com.lms.user.event.PasswordChangedEvent;
import com.lms.user.mapper.UserMapper;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import com.lms.common.service.StorageService;
import com.lms.user.dto.request.UpdateProfileRequest;
import com.lms.user.dto.response.UserProfileResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final String RESOURCE = "USER";

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final AccountStatusService accountStatusService;
    private final SessionService sessionService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;
    private final StorageService storageService;
    private final InvitationRepository invitationRepository;

    @Override
    @Transactional
    public UserResponse update(UUID id, UpdateUserRequest request) {
        User user = requireWithAuthorities(id);

        if (StringUtils.hasText(request.getName())) {
            user.setName(request.getName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(StringUtils.hasText(request.getProfileImageUrl())
                    ? request.getProfileImageUrl().trim() : null);
        }
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateRoles(UUID id, UpdateUserRolesRequest request) {
        User user = requireWithAuthorities(id);
        UUID actorId = currentActorId();

        Set<String> before = user.roleNames();
        Set<Role> target = roleService.resolveByNames(request.getRoles());
        Set<String> after = target.stream().map(Role::getName).collect(java.util.stream.Collectors
                .toCollection(TreeSet::new));

        guardLastAdmin(user, before, after);

        user.replaceRoles(target, actorId);

        after.stream().filter(role -> !before.contains(role)).forEach(role ->
                auditService.record(actorId, AuditAction.ROLE_ASSIGNED, RESOURCE, user.getId(), role));

        before.stream().filter(role -> !after.contains(role)).forEach(role ->
                auditService.record(actorId, AuditAction.ROLE_REMOVED, RESOURCE, user.getId(), role));

        log.info("Roles for {} changed from {} to {}", user.getEmail(), before, after);
        return userMapper.toResponse(user);
    }

    @Override
    public UserResponse findById(UUID id) {
        return userMapper.toResponse(requireWithAuthorities(id));
    }

    @Override
    public PageResponse<UserResponse> search(String search, Boolean active, Pageable pageable) {
        String term = StringUtils.hasText(search) ? search.trim() : null;
        return PageResponse.from(userRepository.search(term, active, PageRequests.sanitize(pageable)),
                userMapper::toResponse);
    }

    @Override
    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = require(id);

        if (user.getPassword() == null) {
            throw new BusinessRuleException("This account has no password yet; accept the invitation first");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessRuleException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessRuleException("New password must differ from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        // Every other session is invalidated: a password change is the point at
        // which previously issued refresh tokens should stop working.
        sessionService.revokeAllForUser(user.getId());

        // Closes out onboarding if this was the temporary password being replaced.
        eventPublisher.publishEvent(new PasswordChangedEvent(user.getId()));

        auditService.record(currentActorId(), AuditAction.PASSWORD_CHANGED, RESOURCE, user.getId(), null);
        log.info("Password changed for {}", user.getEmail());
    }

    @Override
    @Transactional
    public UserResponse deactivate(UUID id, String reason) {
        User user = requireWithAuthorities(id);
        guardLastAdminRemoval(user);

        accountStatusService.deactivate(user, currentActorId(), reason);
        sessionService.revokeAllForUser(user.getId());
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse activate(UUID id, String reason) {
        User user = requireWithAuthorities(id);

        if (user.getPassword() == null) {
            throw new BusinessRuleException(
                    "This account has never been activated; resend the invitation instead");
        }

        accountStatusService.activate(user, currentActorId(), reason);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse lock(UUID id, String reason) {
        User user = requireWithAuthorities(id);
        guardLastAdminRemoval(user);

        accountStatusService.lock(user, currentActorId(), reason);
        sessionService.revokeAllForUser(user.getId());
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse unlock(UUID id, String reason) {
        User user = requireWithAuthorities(id);
        accountStatusService.unlock(user, currentActorId(), reason);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = requireWithAuthorities(id);
        guardLastAdminRemoval(user);
        sessionService.revokeAllForUser(user.getId());
        // Remove all invitations associated with this user before deletion
        invitationRepository.deleteAllByUserId(user.getId());
        try {
            userRepository.delete(user);
            userRepository.flush();
        } catch (Exception e) {
            // Hard delete failed due to FK constraints; soft-delete instead
            accountStatusService.deactivate(user, currentActorId(), "Account removed by administrator");
            user.setActive(false);
            user.setLocked(true);
            userRepository.save(user);
        }
    }

    @Override
    public List<AccountStatusHistoryResponse> statusHistory(UUID id) {
        return accountStatusService.history(id).stream()
                .map(entry -> new AccountStatusHistoryResponse(entry.getId(), entry.getStatus(),
                        entry.getChangedBy(), entry.getReason(), entry.getChangedAt()))
                .toList();
    }

    @Override
    public User requireWithAuthorities(UUID id) {
        return userRepository.findByIdWithAuthorities(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private User require(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private UUID currentActorId() {
        return AuthenticationService.currentPrincipal().map(LmsUserDetails::getUserId).orElse(null);
    }

    /** Refuses a role change that would leave the platform with no administrator. */
    private void guardLastAdmin(User user, Set<String> before, Set<String> after) {
        boolean losingAdmin = before.contains(SystemRoles.ADMIN) && !after.contains(SystemRoles.ADMIN);
        if (losingAdmin && userRepository.countByRoleName(SystemRoles.ADMIN) <= 1) {
            throw new BusinessRuleException("The last administrator cannot have the ADMIN role removed");
        }
    }

    /** Refuses to disable the only remaining administrator. */
    private void guardLastAdminRemoval(User user) {
        if (user.hasRole(SystemRoles.ADMIN) && userRepository.countByRoleName(SystemRoles.ADMIN) <= 1) {
            throw new BusinessRuleException("The last administrator cannot be locked or deactivated");
        }
    }

    @Override
    public UserProfileResponse getProfile(UUID id) {
        User user = requireWithAuthorities(id);
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = requireWithAuthorities(id);

        String newName = request.getFullName();
        if (StringUtils.hasText(newName)) {
            user.setName(newName.trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null);
        }
        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(StringUtils.hasText(request.getProfileImageUrl())
                    ? request.getProfileImageUrl().trim() : null);
        }
        return toProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateAvatar(UUID id, MultipartFile file) {
        User user = requireWithAuthorities(id);
        if (file != null && !file.isEmpty()) {
            try {
                String contentType = file.getContentType() != null ? file.getContentType() : "image/png";
                String key = "avatars/" + user.getId() + "_" + System.currentTimeMillis() + ".png";
                String url = null;
                try {
                    storageService.uploadFile(key, file.getInputStream(), file.getSize(), contentType);
                    url = storageService.getPublicUrl(key);
                } catch (Exception storageException) {
                    log.warn("Storage service upload skipped/failed: {}. Falling back to inline data URL.", storageException.getMessage());
                }
                if (url == null || url.isBlank()) {
                    String base64 = Base64.getEncoder().encodeToString(file.getBytes());
                    url = "data:" + contentType + ";base64," + base64;
                }
                user.setProfileImageUrl(url);
            } catch (Exception e) {
                log.error("Failed to process avatar file upload: {}", e.getMessage(), e);
                throw new BusinessRuleException("Failed to upload avatar: " + e.getMessage());
            }
        }
        return toProfileResponse(user);
    }

    private UserProfileResponse toProfileResponse(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setFullName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setProfileImageUrl(user.getProfileImageUrl());
        response.setAvatarUrl(user.getProfileImageUrl());
        response.setActive(user.isActive());
        response.setLocked(user.isLocked());
        response.setRoles(user.roleNames());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
