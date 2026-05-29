package com.global.order_api.feature.cart;

import com.global.order_api.core.base.BaseMapper;
import org.mapstruct.*;

@Mapper(builder = @Builder(disableBuilder = true))
public interface CartItemMapper extends BaseMapper<CartItemEntity,CartItemRequestDto,CartItemResponseDto> {

    @Mapping(target = "product",ignore = true) /// service will get product from db
    @Mapping(target = "cart",ignore = true) /// service will link it to Cart
    @Mapping(target = "id",ignore = true) /// db auto increment it
    CartItemEntity mapToEntity(CartItemRequestDto dto);


    /// to make him map correctly
    @Mapping(source = "product.id",target = "productId")
    @Mapping(source = "product.name",target = "productName")
    @Mapping(source = "product.price",target = "productPrice")
    @Mapping(target = "subTotal", ignore = true)
    CartItemResponseDto mapToDto(CartItemEntity entity);

    @AfterMapping
    default void calculateSubTotal(CartItemEntity entity, @MappingTarget CartItemResponseDto dto)
    {
        if(entity.getQuantity() !=null && entity.getProduct() != null
        && entity.getProduct().getPrice() !=null)
        {
            double price =entity.getProduct().getPrice().doubleValue(); /// to get value because bigdeciaml
            dto.setSubTotal(entity.getQuantity() * price);
        } else {
            dto.setSubTotal(0.0);
        }

    }
    /// RAW ITEM
    @Mapping(source = "id", target = "cartItemId")
    @Mapping(source = "product.id", target = "productId")
    RawCartItemDto mapToRawDto(CartItemEntity entity);
}
