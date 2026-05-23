package com.global.order_api.feature.product;


import java.math.BigDecimal;

import com.global.order_api.core.base.BaseFilterRequestDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/// FOR ADVANCED SEARCH (MORE FILTERS)
public class ProductFilterRequest extends BaseFilterRequestDto {
	
	private Long categoryId;
	private BigDecimal minPrice;
	private BigDecimal maxPrice;
	private Boolean inStockOnly;

	@Override
	public String generateCacheKey() {
		String baseKey = super.generateCacheKey();

		return baseKey +
				"-cat:" + (categoryId != null ? categoryId : "all") +
				"-minPrice:" + (minPrice != null ? minPrice : "0") +
				"-maxPrice:" + (maxPrice != null ? maxPrice : "any") +
				"-stock:" + (inStockOnly != null ? inStockOnly : "all");
	}
	
}
