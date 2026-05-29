package com.global.order_api.feature.cart;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RawCartDto {
    private Long cartId;
    private List<RawCartItemDto> items;
}