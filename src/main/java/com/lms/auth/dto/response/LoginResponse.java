package com.lms.auth.dto.response;

import com.lms.user.dto.response.UserResponse;

/**
 * Result of a successful sign-in.
 *
 * <p>When {@code mustChangePassword} is true the account is still on the
 * temporary password issued at invite time. The tokens are real but carry no
 * permissions, so the client should route straight to the change-password
 * screen: nothing else will be authorized until the password is replaced.
 */
public class LoginResponse {

    private AuthTokens tokens;

    private UserResponse user;

    private boolean mustChangePassword;

    public LoginResponse() {
    }

    public LoginResponse(AuthTokens tokens, UserResponse user, boolean mustChangePassword) {
        this.tokens = tokens;
        this.user = user;
        this.mustChangePassword = mustChangePassword;
    }

    public AuthTokens getTokens() {
        return tokens;
    }

    public void setTokens(AuthTokens tokens) {
        this.tokens = tokens;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }
}
