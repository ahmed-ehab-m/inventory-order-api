package com.global.order_api.feature.order;

import com.global.order_api.core.base.BaseResponseDto;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter

public class OrderResponseDto extends BaseResponseDto<Long> {
    private OrderStatus status;
    private BigDecimal totalPrice;
    private String shippingAddress;
    private String orderNotes;
    private Long userId;
    private String customerEmail;
    private PaymentType paymentType;
    private List<OrderItemResponseDto> orderItems;
}
