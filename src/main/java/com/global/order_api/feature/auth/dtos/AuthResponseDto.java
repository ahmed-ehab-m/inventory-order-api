package com.global.order_api.feature.auth;

import com.global.order_api.feature.user.dto.UserResponseDto;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
/// to add the token to user details
public class AuthResponseDto {
    private String accessToken;
    private String refreshToken;
    private UserResponseDto user;
}
