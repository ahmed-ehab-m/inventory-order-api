package com.global.order_api.feature.user.mapper;

import com.global.order_api.core.base.BaseMapper;
import com.global.order_api.feature.auth.dtos.RegisterRequestDto;
import com.global.order_api.feature.auth.dtos.UserResponseDto;
import com.global.order_api.feature.user.entity.UserEntity;
import org.mapstruct.Mapper;

@Mapper // trigger map struct to generate code
public interface UserMapper extends BaseMapper<UserEntity, RegisterRequestDto, UserResponseDto> {

}
