package com.global.order_api.feature.auth.dtos;

import com.fasterxml.jackson.annotation.JsonView;
import com.global.order_api.feature.auth.ValidationGroups;
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

public class UserRequestDto {

    @JsonView(DtoViews.OnRegister.class) /// for Swagger
    @NotBlank(message = "{validation.field.required}", groups = {ValidationGroups.OnRegister.class, ValidationGroups.OnUpdate.class})
    @Size(min = 2, max = 50, message = "{validation.name.size}")
    private String name;

    @JsonView(DtoViews.OnLogin.class) /// for Swagger
    @Email(message = "{validation.email.invalid}", groups = {ValidationGroups.OnRegister.class, ValidationGroups.OnLogin.class, ValidationGroups.OnUpdate.class})
    @NotBlank(message = "{validation.field.required}")
    private String email;

    @JsonView(DtoViews.OnLogin.class) /// for Swagger
    @NotBlank(message = "{validation.field.required}", groups = {ValidationGroups.OnRegister.class, ValidationGroups.OnLogin.class,})
    @Size(min = 8, message = "{validation.password.size}")
    private String password;

    @JsonView(DtoViews.OnRegister.class) /// for Swagger
    private String location;

    @JsonView(DtoViews.OnRegister.class) /// for Swagger
    @Pattern(regexp = "^(010|011|012|015)\\d{8}$", message = "{validation.phone.invalid}")
    private String phone;

    /// id not here => because may user edit another user data
    /// id in response dto only
}
