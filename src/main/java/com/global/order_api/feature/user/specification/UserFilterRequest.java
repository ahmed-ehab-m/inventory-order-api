package com.global.order_api.feature.user.specification;

import com.global.order_api.core.base.BaseFilterRequestDto;
import com.global.order_api.feature.user.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFilterRequest extends BaseFilterRequestDto {
    private String userName;
    private String location;
    private UserRole role;
    private Boolean isDeleted;
}
