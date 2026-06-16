package com.global.order_api.feature.category;

import com.global.order_api.core.base.BaseMapper;
import org.mapstruct.Mapper;

// interface because map struct who creates impl im compile time 
// not work with classes
@Mapper
public interface CategoryMapper extends BaseMapper<CategoryEntity, CategoryRequestDto, CategoryResponseDto> {

}
