package com.lms.common.constants;

/** Application-wide API path constants. */
public final class ApiPaths {

    public static final String API_V1 = "/api/v1";

    public static final String AUTH = API_V1 + "/auth";
    public static final String USERS = API_V1 + "/users";
    public static final String ROLES = API_V1 + "/roles";
    public static final String PERMISSIONS = API_V1 + "/permissions";
    public static final String INVITATIONS = API_V1 + "/invitations";

    private ApiPaths() {
    }
}
