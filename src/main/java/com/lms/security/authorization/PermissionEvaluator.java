package com.lms.security.authorization;

import com.lms.security.authentication.LmsUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Locale;

/**
 * Evaluates fine-grained permissions after authentication.
 *
 * <p>Supports both forms of the Spring Security expression:
 * <pre>
 *   hasPermission(course, 'COURSE_UPDATE')
 *   hasPermission(courseId, 'Course', 'UPDATE')
 * </pre>
 */
@Slf4j
@Component("permissionEvaluator")
public class PermissionEvaluator implements org.springframework.security.access.PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return holds(authentication, String.valueOf(permission));
    }

    @Override
    public boolean hasPermission(Authentication authentication,
                                 Serializable targetId,
                                 String targetType,
                                 Object permission) {
        String required = (targetType + "_" + permission).toUpperCase(Locale.ROOT);
        return holds(authentication, required);
    }

    /** Callable from expressions as {@code @permissionEvaluator.holds(authentication, 'COURSE_CREATE')}. */
    public boolean holds(Authentication authentication, String permission) {
        if (authentication == null || !authentication.isAuthenticated() || permission == null) {
            return false;
        }
        if (authentication.getPrincipal() instanceof LmsUserDetails principal) {
            return principal.getPermissions().contains(permission.toUpperCase(Locale.ROOT));
        }
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equalsIgnoreCase(permission));
    }
}
