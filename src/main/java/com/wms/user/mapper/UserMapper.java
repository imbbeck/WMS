package com.wms.user.mapper;

import com.wms.user.domain.model.UserInfo;
import com.wms.user.dto.UserRequest;
import com.wms.user.dto.UserResponse;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toResponse(UserInfo userInfo);
} 