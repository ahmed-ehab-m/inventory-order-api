package com.global.order_api.feature.product;

import java.math.BigDecimal;


import com.global.order_api.core.base.BaseResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class UserProductResponseDto extends BaseResponseDto<Long> {
	private String name;
	private String description;
	private String image;
	private BigDecimal price;
//	private int stockCount;

	private Long categoryId;
	private String categoryName;
}
