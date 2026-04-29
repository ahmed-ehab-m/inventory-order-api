package com.global.order_api.feature.order;

import com.global.order_api.core.base.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/// child entity
/// want only map to Dto and we don't have OrderRequestDto
@Mapper
public interface OrderItemMappper {

    @Mapping(source = "product.id",target="productId")
    @Mapping(source = "product.name",target="productName")

    /// calc subtotal during mapping instead of calc it in service
    @Mapping(target = "subTotal",expression =
            "java(entity.getPrice().multiply (new java.math.BigDecimal(entity.getQuantity())))")
    OrderItemResponseDto toDto(OrderItemEntity entity);
}
