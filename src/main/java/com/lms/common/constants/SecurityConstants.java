package com.lms.common.constants;

/** Constants shared by the security infrastructure. */
public final class SecurityConstants {

    public static final String AUTH_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    /** Access token claims. */
    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_SESSION_ID = "sid";
    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_PERMISSIONS = "permissions";
    public static final String CLAIM_TOKEN_TYPE = "typ";

    /**
     * Only the access token is a JWT. The refresh token is opaque random
     * material whose digest lives in {@code user_session}.
     */
    public static final String TOKEN_TYPE_ACCESS = "access";

    public static final String ROLE_PREFIX = "ROLE_";

    /**
     * The only authority carried by a token issued to someone still holding a
     * temporary password. It grants nothing except the ability to replace that
     * password, so every permission-guarded endpoint denies it automatically.
     */
    public static final String PASSWORD_CHANGE_ONLY = "PASSWORD_CHANGE_ONLY";

    private SecurityConstants() {
    }
}
