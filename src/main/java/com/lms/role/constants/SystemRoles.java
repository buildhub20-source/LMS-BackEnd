package com.lms.role.constants;

import java.util.Set;

/**
 * The three roles the authorization model is seeded with.
 *
 * <p>The ERD has no {@code system_role} column, so protection is enforced here
 * by name rather than by a database flag.
 */
public final class SystemRoles {

    public static final String ADMIN = "ADMIN";
    public static final String INSTRUCTOR = "INSTRUCTOR";
    public static final String STUDENT = "STUDENT";

    public static final Set<String> PROTECTED = Set.of(ADMIN, INSTRUCTOR, STUDENT);

    private SystemRoles() {
    }

    public static boolean isProtected(String roleName) {
        return PROTECTED.contains(roleName);
    }
}
