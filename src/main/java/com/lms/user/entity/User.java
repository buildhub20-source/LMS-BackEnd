package com.lms.user.entity;

import com.lms.common.audit.Timestamped;
import com.lms.role.entity.Role;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * An LMS user account.
 *
 * <p>{@code password} is null between the moment an administrator invites the
 * user and the moment the invitation is accepted. Such an account can never
 * authenticate because {@code active} is also false until activation.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends Timestamped {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /** BCrypt hash. Null until the invited user sets a password. */
    @Column(name = "password", length = 255)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = false;

    @Builder.Default
    @Column(name = "is_locked", nullable = false)
    private boolean locked = false;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserRole> userRoles = new HashSet<>();

    /** True only when the account may exchange credentials for tokens. */
    public boolean canAuthenticate() {
        return active && !locked && password != null;
    }

    public Set<Role> roles() {
        return userRoles.stream().map(UserRole::getRole).collect(Collectors.toSet());
    }

    public Set<String> roleNames() {
        return userRoles.stream()
                .map(userRole -> userRole.getRole().getName())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public Set<String> permissionNames() {
        return userRoles.stream()
                .flatMap(userRole -> userRole.getRole().getPermissions().stream())
                .map(permission -> permission.getName())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public boolean hasRole(String roleName) {
        return userRoles.stream().anyMatch(userRole -> userRole.getRole().getName().equals(roleName));
    }

    public void assignRole(Role role, UUID assignedBy) {
        if (!hasRole(role.getName())) {
            userRoles.add(UserRole.of(this, role, assignedBy));
        }
    }

    public void removeRole(String roleName) {
        userRoles.removeIf(userRole -> userRole.getRole().getName().equals(roleName));
    }

    public void replaceRoles(Set<Role> newRoles, UUID assignedBy) {
        userRoles.removeIf(userRole -> newRoles.stream()
                .noneMatch(role -> role.getId().equals(userRole.getRole().getId())));

        newRoles.forEach(role -> assignRole(role, assignedBy));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof User user)) {
            return false;
        }
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
