package com.global.order_api.feature.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter

public class RegisterRequestDto {

    @NotBlank(message = "{validation.field.required}")
    @Size(min = 2, max = 50, message = "{validation.name.size}")
    private String name;

    @Email(message = "{validation.email.invalid}")
    @NotBlank(message = "{validation.field.required}")
    private String email;

    @NotBlank(message = "{validation.field.required}")
    @Size(min = 8, message = "{validation.password.size}")
    private String password;

    private String location;

    @Pattern(regexp = "^(010|011|012|015)\\d{8}$", message = "{validation.phone.invalid}")
    private String phone;

    /// id not here => because may user edit another user data
    /// id in response dto only
}
