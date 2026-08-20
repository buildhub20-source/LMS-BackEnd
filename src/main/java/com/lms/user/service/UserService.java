package com.lms.user.service;

import com.lms.common.response.PageResponse;
import com.lms.user.dto.request.ChangePasswordRequest;
import com.lms.user.dto.request.UpdateUserRequest;
import com.lms.user.dto.request.UpdateUserRolesRequest;
import com.lms.user.dto.response.AccountStatusHistoryResponse;
import com.lms.user.dto.response.UserResponse;
import com.lms.user.entity.User;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * User management use cases.
 *
 * <p>There is no create operation here: accounts are provisioned through the
 * invitation flow, which is the only path that produces an activated account.
 */
public interface UserService {

    UserResponse update(UUID id, UpdateUserRequest request);

    UserResponse updateRoles(UUID id, UpdateUserRolesRequest request);

    UserResponse findById(UUID id);

    PageResponse<UserResponse> search(String search, Boolean active, Pageable pageable);

    void changePassword(UUID id, ChangePasswordRequest request);

    UserResponse deactivate(UUID id, String reason);

    UserResponse activate(UUID id, String reason);

    UserResponse lock(UUID id, String reason);

    UserResponse unlock(UUID id, String reason);

    List<AccountStatusHistoryResponse> statusHistory(UUID id);

    User requireWithAuthorities(UUID id);
}
