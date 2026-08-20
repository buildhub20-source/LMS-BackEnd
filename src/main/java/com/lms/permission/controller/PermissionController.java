package com.lms.permission.controller;

import com.lms.common.constants.ApiPaths;
import com.lms.common.response.ApiResponse;
import com.lms.permission.dto.request.CreatePermissionRequest;
import com.lms.permission.dto.request.UpdatePermissionRequest;
import com.lms.permission.dto.response.PermissionResponse;
import com.lms.permission.service.PermissionService;
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

/** Permission management endpoints. */
@Tag(name = "Permissions")
@RestController
@RequestMapping(ApiPaths.PERMISSIONS)
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(summary = "Create a permission")
    @PostMapping
    @PreAuthorize("hasAuthority('PERMISSIONS_MANAGE')")
    public ResponseEntity<ApiResponse<PermissionResponse>> create(
            @Valid @RequestBody CreatePermissionRequest request) {

        PermissionResponse created = permissionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(created, "Permission created"));
    }

    @Operation(summary = "List all permissions")
    @GetMapping
    @PreAuthorize("hasAuthority('PERMISSIONS_VIEW')")
    public ResponseEntity<ApiResponse<List<PermissionResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.of(permissionService.findAll()));
    }

    @Operation(summary = "Fetch one permission")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSIONS_VIEW')")
    public ResponseEntity<ApiResponse<PermissionResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(permissionService.findById(id)));
    }

    @Operation(summary = "Update a permission description")
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSIONS_MANAGE')")
    public ResponseEntity<ApiResponse<PermissionResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdatePermissionRequest request) {

        return ResponseEntity.ok(ApiResponse.of(permissionService.update(id, request)));
    }

    @Operation(summary = "Delete a permission")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERMISSIONS_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        permissionService.delete(id);
        return ResponseEntity.ok(ApiResponse.message("Permission deleted"));
    }
}
