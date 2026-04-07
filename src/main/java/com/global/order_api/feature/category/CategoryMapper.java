package com.global.order_api.feature.category;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.stereotype.Component;

import com.global.order_api.core.base.BaseMapper;

// interface because map struct who creates impl im compile time 
// not work with classes
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryEntity,CategoryRequestDto ,CategoryResponseDto>{

	}
