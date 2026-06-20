package com.global.order_api.feature.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RawCartItemDto {
    private Long cartItemId;
    private Long productId;
    private Integer quantity;
}