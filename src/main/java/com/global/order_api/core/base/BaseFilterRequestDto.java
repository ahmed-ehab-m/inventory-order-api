package com.global.order_api.core.base;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaseFilterRequestDto {
	
	private int page=0;
	private int size=10;
	private String sortBy="createdAt";
	private String sortDirection="DESC";
	
}
