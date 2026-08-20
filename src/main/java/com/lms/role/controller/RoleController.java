package com.lms.role.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.role.dto.request.CreateRoleRequest;
import com.lms.role.dto.request.UpdateRoleRequest;
import com.lms.role.dto.response.RoleResponse;
import com.lms.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Role management endpoints. */
@Tag(name = "Roles")
@RestController
@RequestMapping(ApiPaths.ROLES)
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "Create a role")
    @PostMapping
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    public ResponseEntity<ApiResponse<RoleResponse>> create(@Valid @RequestBody CreateRoleRequest request) {
        RoleResponse created = roleService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created, "Role created"));
    }

    @Operation(summary = "List all roles")
    @GetMapping
    @PreAuthorize("hasAuthority('ROLES_VIEW')")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.of(roleService.findAll()));
    }

    @Operation(summary = "Fetch one role with its permissions")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_VIEW')")
    public ResponseEntity<ApiResponse<RoleResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(roleService.findById(id)));
    }

    @Operation(summary = "Update a role description or its permissions")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    public ResponseEntity<ApiResponse<RoleResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateRoleRequest request) {

        return ResponseEntity.ok(ApiResponse.of(roleService.update(id, request)));
    }

    @Operation(summary = "Delete a role", description = "System roles cannot be deleted")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLES_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        roleService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Role deleted"));
    }
}
