package com.global.order_api.feature.cart;

import com.global.order_api.core.base.BaseResponseDto;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class CartItemResponseDto extends BaseResponseDto<Long> {
    private Long productId;
    private String productName;
    private String productPrice;
    private Integer quantity;
    private Double subTotal;
}
