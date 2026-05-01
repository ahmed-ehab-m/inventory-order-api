package com.global.order_api.feature.order;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class OrderRequestDto {

    private String shippingAddress;
    private String orderNotes;
}
