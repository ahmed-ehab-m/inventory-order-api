package com.global.order_api.feature.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class CartItemRequestDto {
    @NotNull(message = "{validation.field.required}")
    @JsonProperty("product_id")
    private Long productId;

    @NotNull(message = "{validation.field.required}")
    @Min(value = 1, message = "{validation.field.positive}")
    private Integer quantity;
}
