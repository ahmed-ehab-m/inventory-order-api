package com.global.order_api.feature.auth.dtos;

import com.global.order_api.core.base.BaseResponseDto;
import com.global.order_api.feature.user.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
/// to see the id field
public class UserResponseDto extends BaseResponseDto<Long> {
    private String name;
    private String email;
    private String location;
    private String phone;
    private UserRole role;
}
