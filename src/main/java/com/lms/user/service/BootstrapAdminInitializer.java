package com.lms.user.service;

import com.lms.role.constants.SystemRoles;
import com.lms.role.service.RoleService;
import com.lms.user.entity.AccountStatus;
import com.lms.user.entity.User;
import com.lms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Creates the first administrator so the platform can be bootstrapped.
 *
 * <p>Every other account is provisioned through the invitation flow; this
 * exists only to break the circular dependency where inviting a user requires
 * an authenticated administrator. Disabled unless
 * {@code lms.bootstrap.admin.enabled} is true, and it never touches an existing
 * account.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "lms.bootstrap.admin", name = "enabled", havingValue = "true")
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleService roleService;
    private final AccountStatusService accountStatusService;
    private final PasswordEncoder passwordEncoder;

    @Value("${lms.bootstrap.admin.email:}")
    private String email;

    @Value("${lms.bootstrap.admin.password:}")
    private String password;

    @Value("${lms.bootstrap.admin.name:Platform Administrator}")
    private String name;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            log.warn("Bootstrap admin is enabled but email or password is not configured; skipping");
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            log.debug("Bootstrap admin {} already exists", email);
            return;
        }

        User admin = User.builder()
                .name(name)
                .email(email.toLowerCase())
                .password(passwordEncoder.encode(password))
                .active(true)
                .locked(false)
                .build();

        User saved = userRepository.save(admin);
        saved.assignRole(roleService.requireByName(SystemRoles.ADMIN), null);

        accountStatusService.recordTransition(saved.getId(), AccountStatus.ACTIVE, null,
                "Bootstrap administrator created at startup");

        log.info("Bootstrap administrator {} created", saved.getEmail());
    }
}
