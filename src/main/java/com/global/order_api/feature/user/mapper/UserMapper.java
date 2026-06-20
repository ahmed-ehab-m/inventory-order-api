package com.global.order_api.feature.user.mapper;

import com.global.order_api.core.base.BaseMapper;
import com.global.order_api.feature.user.dto.UserRequestDto;
import com.global.order_api.feature.user.dto.UserResponseDto;
import com.global.order_api.feature.user.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper // trigger map struct to generate code
public interface UserMapper extends BaseMapper<UserEntity, UserRequestDto, UserResponseDto> {

}
