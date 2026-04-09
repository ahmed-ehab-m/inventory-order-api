package com.global.order_api.feature.product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.global.order_api.core.base.BaseMapper;

@Mapper // trigger map struct to generate code
// in compile time
// map struct generate productmapperimpl
public interface ProductMapper extends BaseMapper<ProductEntity, ProductRequestDto, ProductResponseDto>{
	@Override
	// to make him map correctly
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    ProductResponseDto mapToDto(ProductEntity entity);
	
	@Override
	/// tell mapper ignore Category in product entity 
    @Mapping(target = "category", ignore = true)
    ProductEntity mapToEntity(ProductRequestDto requestDto);
}

	