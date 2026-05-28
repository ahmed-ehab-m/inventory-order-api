package com.global.order_api.feature.product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.global.order_api.core.base.BaseMapper;

import java.util.List;

@Mapper // trigger map struct to generate code
// in compile time
// map struct generate productmapperimpl
public interface ProductMapper extends BaseMapper<ProductEntity, ProductRequestDto, UserProductResponseDto>{
	@Override
	// to make him map correctly
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    UserProductResponseDto mapToDto(ProductEntity entity);
	
	@Override
	/// tell mapper ignore Category in product entity 
    @Mapping(target = "category", ignore = true)
    ProductEntity mapToEntity(ProductRequestDto requestDto);

    List<AdminProductResponseDto> mapToAdminDtoList(List<ProductEntity> entities);
    AdminProductResponseDto mapToAdminDto(ProductEntity entity);
}

	