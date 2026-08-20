package com.lms.role.service;

import com.lms.role.dto.request.CreateRoleRequest;
import com.lms.role.dto.request.UpdateRoleRequest;
import com.lms.role.dto.response.RoleResponse;
import com.lms.role.entity.Role;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Role management use cases. */
public interface RoleService {

    RoleResponse create(CreateRoleRequest request);

    RoleResponse update(UUID id, UpdateRoleRequest request);

    RoleResponse findById(UUID id);

    List<RoleResponse> findAll();

    void delete(UUID id);

    /** Resolves role names to entities, failing if any name is unknown. */
    Set<Role> resolveByNames(Set<String> names);

    Role requireByName(String name);
}
