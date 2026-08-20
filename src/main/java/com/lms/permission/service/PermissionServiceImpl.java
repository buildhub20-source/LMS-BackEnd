package com.lms.permission.service;

import com.lms.common.exception.ResourceAlreadyExistsException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.permission.dto.request.CreatePermissionRequest;
import com.lms.permission.dto.request.UpdatePermissionRequest;
import com.lms.permission.dto.response.PermissionResponse;
import com.lms.permission.entity.Permission;
import com.lms.permission.mapper.PermissionMapper;
import com.lms.permission.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;

    @Override
    @Transactional
    public PermissionResponse create(CreatePermissionRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw ResourceAlreadyExistsException.of("Permission", request.getName());
        }

        Permission permission = Permission.builder()
                .name(request.getName())
                .resource(request.getResource())
                .action(request.getAction())
                .description(request.getDescription())
                .build();

        log.info("Creating permission {}", request.getName());
        return permissionMapper.toResponse(permissionRepository.save(permission));
    }

    @Override
    @Transactional
    public PermissionResponse update(UUID id, UpdatePermissionRequest request) {
        Permission permission = require(id);
        permission.setDescription(request.getDescription());
        return permissionMapper.toResponse(permission);
    }

    @Override
    public PermissionResponse findById(UUID id) {
        return permissionMapper.toResponse(require(id));
    }

    @Override
    public List<PermissionResponse> findAll() {
        return permissionMapper.toResponseList(permissionRepository.findAll(Sort.by("name")));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Permission permission = require(id);
        log.info("Deleting permission {}", permission.getName());
        permissionRepository.delete(permission);
    }

    @Override
    public Set<Permission> resolveByNames(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }

        Set<Permission> found = permissionRepository.findAllByNameIn(names);
        if (found.size() != names.size()) {
            Set<String> foundNames = found.stream().map(Permission::getName).collect(Collectors.toSet());
            Set<String> missing = names.stream().filter(name -> !foundNames.contains(name))
                    .collect(Collectors.toCollection(java.util.TreeSet::new));
            throw ResourceNotFoundException.of("Permission", missing);
        }
        return found;
    }

    private Permission require(UUID id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Permission", id));
    }
}
