package com.global.order_api.feature.user;

import com.global.order_api.feature.auth.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter

public class ChangePasswordRequestDto {

    @NotBlank(message = "{validation.field.required}", groups = {ValidationGroups.OnRegister.class, ValidationGroups.OnLogin.class,})
    @Size(min = 8, message = "{validation.password.size}")
    private String oldPassword;

    @NotBlank(message = "{validation.field.required}", groups = {ValidationGroups.OnRegister.class, ValidationGroups.OnLogin.class,})
    @Size(min = 8, message = "{validation.password.size}")
    private String newPassword;
}
