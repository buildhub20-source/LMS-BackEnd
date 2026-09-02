package com.lms.platform.service;

import com.lms.common.exception.ApplicationException;
import com.lms.common.exception.ErrorCode;
import com.lms.config.PlatformConfig;
import com.lms.platform.dto.PlatformLoginRequest;
import com.lms.platform.dto.PlatformLoginResponse;
import com.lms.platform.entity.PlatformAdmin;
import com.lms.platform.repository.PlatformAdminRepository;
import com.lms.platform.security.PlatformAdminPrincipal;
import com.lms.platform.security.PlatformJwtService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class PlatformAdminService {
    private final PlatformAdminRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformJwtService jwtService;
    private final PlatformConfig config;

    public PlatformAdminService(PlatformAdminRepository repository, PasswordEncoder passwordEncoder,
                                PlatformJwtService jwtService, PlatformConfig config) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.config = config;
    }

    public PlatformLoginResponse login(PlatformLoginRequest request) {
        String email = request.email().trim().toLowerCase();
        String configuredGlobalEmail = config.getGlobalAdmin().getEmail() == null ? ""
                : config.getGlobalAdmin().getEmail().trim().toLowerCase();
        // The control plane has one configured global administrator. This also
        // prevents a legacy tenant administrator record from gaining platform access.
        if (!config.getGlobalAdmin().isEnabled() || !email.equals(configuredGlobalEmail)) {
            throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }
        PlatformAdmin admin = repository.findByEmail(email)
                .orElseThrow(() -> new ApplicationException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials"));
        if (!admin.isActive() || !passwordEncoder.matches(request.password(), admin.getPasswordHash())) {
            throw new ApplicationException(ErrorCode.INVALID_CREDENTIALS, "Invalid credentials");
        }
        PlatformAdminPrincipal principal = new PlatformAdminPrincipal(admin.getId(), admin.getEmail(), true);
        return new PlatformLoginResponse(jwtService.issue(principal),
                Instant.now().plusSeconds(15 * 60));
    }

    @Bean
    ApplicationRunner bootstrapPlatformAdmin() {
        return args -> {
            PlatformConfig.GlobalAdmin bootstrap = config.getGlobalAdmin();
            if (!config.isEnabled() || !bootstrap.isEnabled()) return;
            if (bootstrap.getEmail() == null || bootstrap.getEmail().isBlank()
                    || bootstrap.getPassword() == null || bootstrap.getPassword().isBlank()) {
                throw new IllegalStateException("PLATFORM_ADMIN_EMAIL and PLATFORM_ADMIN_PASSWORD are required");
            }
            String email = bootstrap.getEmail().trim().toLowerCase();
            if (repository.findByEmail(email).isEmpty()) {
                repository.save(PlatformAdmin.builder().name(bootstrap.getName()).email(email)
                        .passwordHash(passwordEncoder.encode(bootstrap.getPassword())).active(true).build());
            }
        };
    }
}
