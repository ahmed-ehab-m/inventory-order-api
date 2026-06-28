package com.global.order_api.feature.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDto {

    @Email(message = "{validation.email.invalid}")
    @NotBlank(message = "{validation.field.required}")
    private String email;

    @NotBlank(message = "{validation.field.required}")
    private String password;
}