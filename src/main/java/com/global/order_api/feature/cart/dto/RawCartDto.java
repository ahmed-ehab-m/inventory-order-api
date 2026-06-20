package com.global.order_api.feature.cart.dto;

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
    private Long userId;
    private Long cartId;
    private List<RawCartItemDto> items;
}