package com.global.order_api.feature.user;

import com.global.order_api.core.base.BaseFilterRequestDto;
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
