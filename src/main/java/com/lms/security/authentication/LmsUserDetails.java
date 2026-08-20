package com.lms.security.authentication;

import com.lms.common.constants.SecurityConstants;
import com.lms.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Security principal. Carries the roles and permissions resolved at
 * authentication time so authorization checks never hit the database.
 */
@Getter
public class LmsUserDetails implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String name;
    private final String password;
    private final boolean active;
    private final boolean locked;
    private final Set<String> roles;
    private final Set<String> permissions;
    private final Collection<GrantedAuthority> authorities;

    /** Populated for a principal restored from an access token. */
    private final UUID sessionId;

    public LmsUserDetails(UUID userId,
                          String email,
                          String name,
                          String password,
                          boolean active,
                          boolean locked,
                          Set<String> roles,
                          Set<String> permissions,
                          UUID sessionId) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.password = password;
        this.active = active;
        this.locked = locked;
        this.roles = Set.copyOf(roles);
        this.permissions = Set.copyOf(permissions);
        this.sessionId = sessionId;

        Collection<GrantedAuthority> granted = new LinkedHashSet<>();
        this.roles.forEach(role -> granted.add(new SimpleGrantedAuthority(SecurityConstants.ROLE_PREFIX + role)));
        this.permissions.forEach(permission -> granted.add(new SimpleGrantedAuthority(permission)));
        this.authorities = Set.copyOf(granted);
    }

    /** Builds the principal from a user aggregate loaded with its authorities. */
    public static LmsUserDetails from(User user) {
        return new LmsUserDetails(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPassword(),
                user.isActive(),
                user.isLocked(),
                user.roleNames(),
                user.permissionNames(),
                null);
    }

    /** Builds the principal from access-token claims; carries no credentials. */
    public static LmsUserDetails fromClaims(UUID userId,
                                            String email,
                                            Set<String> roles,
                                            Set<String> permissions,
                                            UUID sessionId) {
        return new LmsUserDetails(userId, email, null, null, true, false, roles, permissions, sessionId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
