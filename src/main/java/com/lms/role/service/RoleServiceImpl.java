package com.lms.role.service;

import com.lms.common.exception.BusinessRuleException;
import com.lms.role.constants.SystemRoles;
import com.lms.common.exception.ResourceAlreadyExistsException;
import com.lms.common.exception.ResourceNotFoundException;
import com.lms.permission.service.PermissionService;
import com.lms.role.dto.request.CreateRoleRequest;
import com.lms.role.dto.request.UpdateRoleRequest;
import com.lms.role.dto.response.RoleResponse;
import com.lms.role.entity.Role;
import com.lms.role.mapper.RoleMapper;
import com.lms.role.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionService permissionService;
    private final RoleMapper roleMapper;

    @Override
    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw ResourceAlreadyExistsException.of("Role", request.getName());
        }

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .permissions(new HashSet<>(permissionService.resolveByNames(request.getPermissions())))
                .build();

        log.info("Creating role {}", request.getName());
        return roleMapper.toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse update(UUID id, UpdateRoleRequest request) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", id));

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getPermissions() != null) {
            role.setPermissions(new HashSet<>(permissionService.resolveByNames(request.getPermissions())));
        }
        return roleMapper.toResponse(role);
    }

    @Override
    public RoleResponse findById(UUID id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", id));
        return roleMapper.toResponse(role);
    }

    @Override
    public List<RoleResponse> findAll() {
        return roleMapper.toResponseList(roleRepository.findAll(Sort.by("name")));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", id));

        if (SystemRoles.isProtected(role.getName())) {
            throw new BusinessRuleException("System role " + role.getName() + " cannot be deleted");
        }

        log.info("Deleting role {}", role.getName());
        roleRepository.delete(role);
    }

    @Override
    public Set<Role> resolveByNames(Set<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }

        Set<Role> resolved = names.stream()
                .map(name -> roleRepository.findByNameWithPermissions(name)
                        .orElseThrow(() -> ResourceNotFoundException.of("Role", name)))
                .collect(Collectors.toCollection(HashSet::new));

        return resolved;
    }

    @Override
    public Role requireByName(String name) {
        return roleRepository.findByNameWithPermissions(name)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", name));
    }
}
