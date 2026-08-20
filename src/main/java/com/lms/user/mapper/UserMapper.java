package com.lms.user.mapper;

import com.lms.user.dto.response.UserResponse;
import com.lms.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(user.roleNames())")
    @Mapping(target = "activated", expression = "java(user.getPassword() != null)")
    UserResponse toResponse(User user);

    List<UserResponse> toResponseList(List<User> users);

    /** Used where the aggregate was loaded without its role graph. */
    default Set<String> roleNames(User user) {
        return user.roleNames();
    }
}
