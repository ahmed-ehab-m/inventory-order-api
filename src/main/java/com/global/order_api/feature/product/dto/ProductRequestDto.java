package com.global.order_api.feature.product.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class ProductRequestDto {

    @NotBlank(message = "{validation.field.required}")
    @Size(max = 255)
    private String name;

    private String description;
//	private String image; // because front-end send image using MultiPartFile

    @NotNull(message = "{validation.field.required}")
    @Positive(message = "{validation.field.positive}")
    private BigDecimal price;

    @NotNull(message = "{validation.field.required}")
    @Min(value = 0, message = "{validation.field.equalsOrGreaterThanZero}")
    private Integer stockCount;

    @NotNull(message = "{validation.field.required}")
    private Long categoryId;
}
