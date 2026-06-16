package com.global.order_api.feature.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequestDto {
    @NotBlank(message = "{validation.field.required}")
    @Size(max = 255)
    private String name;
    private String description;
}
