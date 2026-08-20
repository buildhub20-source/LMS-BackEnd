package com.lms.permission.service;

import com.lms.permission.dto.request.CreatePermissionRequest;
import com.lms.permission.dto.request.UpdatePermissionRequest;
import com.lms.permission.dto.response.PermissionResponse;
import com.lms.permission.entity.Permission;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Permission management use cases. */
public interface PermissionService {

    PermissionResponse create(CreatePermissionRequest request);

    PermissionResponse update(UUID id, UpdatePermissionRequest request);

    PermissionResponse findById(UUID id);

    List<PermissionResponse> findAll();

    void delete(UUID id);

    /** Resolves permission names to entities, failing if any name is unknown. */
    Set<Permission> resolveByNames(Set<String> names);
}
