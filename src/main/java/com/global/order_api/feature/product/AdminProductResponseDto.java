package com.global.order_api.feature.product;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class AdminProductResponseDto {
    private String name;
    private String description;
    private String image;
    private BigDecimal price;
	private int stockCount;

    private Long categoryId;
    private String categoryName;
}
