package com.global.order_api.feature.order.dto;

import com.global.order_api.core.base.BaseResponseDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class OrderItemResponseDto extends BaseResponseDto<Long> {

    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal subTotal; // price * quantity

}

