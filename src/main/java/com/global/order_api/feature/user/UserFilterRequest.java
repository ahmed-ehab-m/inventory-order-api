package com.global.order_api.feature.user;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserFilterRequest {
    private String userName;
    private String location;
    private UserRole role;
}
