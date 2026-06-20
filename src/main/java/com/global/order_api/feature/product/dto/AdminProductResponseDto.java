package com.global.order_api.feature.product.dto;

import com.global.order_api.core.base.BaseResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class AdminProductResponseDto extends BaseResponseDto<Long> {
    private String name;
    private String description;
    private String image;
    private BigDecimal price;
    private int stockCount;

    private Long categoryId;
    private String categoryName;
}
