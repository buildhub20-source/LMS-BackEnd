package com.lms.common.constants;

/** Application-wide API path constants. */
public final class ApiPaths {

    public static final String API_V1 = "/api/v1";

    public static final String AUTH        = API_V1 + "/auth";
    public static final String USERS       = API_V1 + "/users";
    public static final String ROLES       = API_V1 + "/roles";
    public static final String PERMISSIONS = API_V1 + "/permissions";
    public static final String INVITATIONS = API_V1 + "/invitations";
    public static final String COURSES     = API_V1 + "/courses";
    // Assessment module — admin operations
    public static final String ADMIN_ASSESSMENTS  = API_V1 + "/admin/assessments";
    public static final String ADMIN_QUESTIONS    = API_V1 + "/admin/questions";
    public static final String ADMIN_TEST_CASES   = API_V1 + "/admin/test-cases";

    // Assessment module — student operations
    public static final String STUDENT_ASSESSMENTS = API_V1 + "/student/assessments";
    public static final String STUDENT_ATTEMPTS    = API_V1 + "/student/attempts";

    private ApiPaths() {
    }
}
