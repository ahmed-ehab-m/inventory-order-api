package com.global.order_api.feature.user;

import com.global.order_api.core.base.BaseMapper;
import org.mapstruct.Mapper;

@Mapper // trigger map struct to generate code
public interface UserMapper extends BaseMapper<UserEntity,UserRequestDto,UserResponseDto> {\

}
