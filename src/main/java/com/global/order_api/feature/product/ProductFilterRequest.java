package com.global.order_api.feature.product;


import com.global.order_api.core.base.BaseFilterRequestDto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductFilterRequest extends BaseFilterRequestDto {
	private String searchKeyword;
}
