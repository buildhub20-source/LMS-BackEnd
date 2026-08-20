package com.lms.permission.mapper;

import com.lms.permission.dto.response.PermissionResponse;
import com.lms.permission.entity.Permission;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper
public interface PermissionMapper {

    PermissionResponse toResponse(Permission permission);

    List<PermissionResponse> toResponseList(List<Permission> permissions);
}
