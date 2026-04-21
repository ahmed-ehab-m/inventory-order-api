package com.global.order_api.feature.user;

import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
/// to add the token to user details
public class AuthResponseDto {
    private String token;
    private UserResponseDto user;
}
