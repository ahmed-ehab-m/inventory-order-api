package com.global.order_api.feature.cart;

import com.global.order_api.core.base.BaseMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/// map struct uses cart item mapper in map list of cart items
@Mapper(uses = {CartItemMapper.class})
public interface CartMapper extends BaseMapper<CartEntity, Void, CartResponseDto> {

    /// map List<CartItemEntity> to List<CartIremReposneDto>
    @Mapping(source = "items", target = "cartItems")
    @Mapping(target = "totalCartPrice", ignore = true)
    CartResponseDto mapToDto(CartEntity entity);

    @AfterMapping
    default void calculateTotalCartPrice(CartEntity entity, @MappingTarget CartResponseDto dto) {
        if (dto.getCartItems() != null) {
            double total = 0;
            for (CartItemResponseDto cartItem : dto.getCartItems()) {
                total += cartItem.getSubTotal() == null ? 0 : cartItem.getSubTotal();
            }
            dto.setTotalCartPrice(total);
        } else {
            dto.setTotalCartPrice(0.0);
        }
    }

    /// RAW DTO
    @Mapping(source = "id", target = "cartId")
    @Mapping(source = "user.id", target = "userId")
    RawCartDto mapToRawDto(CartEntity entity);
}
