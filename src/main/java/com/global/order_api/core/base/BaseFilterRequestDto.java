package com.global.order_api.core.base;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseFilterRequestDto {
	
	private String searchKeyword;
	/// Default values to avoid NullPointerException
	@Min(value = 0, message = "{error.validation.page.min}")
	private int page=0;
	@Min(value = 1, message = "{error.validation.size.min}")
	private int size=10;
	private String sortBy="createdAt";
	private String sortDirection="DESC";
	
}
