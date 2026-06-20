package com.global.order_api.feature.category.mapper;

import com.global.order_api.core.base.BaseMapper;
import com.global.order_api.feature.category.entity.CategoryEntity;
import com.global.order_api.feature.category.dto.CategoryResponseDto;
import com.global.order_api.feature.category.dto.CategoryRequestDto;
import org.mapstruct.Mapper;

// interface because map struct who creates impl im compile time 
// not work with classes
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryEntity, CategoryRequestDto, CategoryResponseDto> {

}
