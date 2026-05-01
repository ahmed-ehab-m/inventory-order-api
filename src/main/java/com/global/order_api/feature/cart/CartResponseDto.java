package com.global.order_api.feature.cart;

import com.global.order_api.core.base.BaseResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class CartResponseDto extends BaseResponseDto<Long>  {
    private List<CartItemResponseDto> cartItems;
    private Double totalCartPrice;
}
