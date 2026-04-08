package com.global.order_api.feature.product;

import java.math.BigDecimal;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@AllArgsConstructor
public class ProductResponseDto {
	
	private Long id; // important for front-end
	private String name;
	private String description;
	private String image;
	private BigDecimal price;
	private int stockCount;
	
	private Long categoryId;
	private String categoryName;
}
