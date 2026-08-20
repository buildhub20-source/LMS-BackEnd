package com.lms.role.mapper;

import com.lms.permission.entity.Permission;
import com.lms.role.dto.response.RoleResponse;
import com.lms.role.entity.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Mapper
public interface RoleMapper {

    @Mapping(target = "permissions", expression = "java(permissionNames(role))")
    @Mapping(target = "systemRole", expression = "java(com.lms.role.constants.SystemRoles.isProtected(role.getName()))")
    RoleResponse toResponse(Role role);

    List<RoleResponse> toResponseList(List<Role> roles);

    default Set<String> permissionNames(Role role) {
        if (role.getPermissions() == null) {
            return Set.of();
        }
        return role.getPermissions().stream()
                .map(Permission::getName)
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
