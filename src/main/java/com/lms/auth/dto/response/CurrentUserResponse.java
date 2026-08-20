package com.lms.auth.dto.response;

import com.lms.user.dto.response.UserResponse;

import java.util.Set;

/**
 * The authenticated principal plus the permissions the frontend needs to decide
 * what to render. Authorization is still enforced server-side on every call.
 */
public class CurrentUserResponse {

    private UserResponse user;

    private Set<String> roles;

    private Set<String> permissions;

    public CurrentUserResponse() {
    }

    public CurrentUserResponse(UserResponse user, Set<String> roles, Set<String> permissions) {
        this.user = user;
        this.roles = roles;
        this.permissions = permissions;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
}
