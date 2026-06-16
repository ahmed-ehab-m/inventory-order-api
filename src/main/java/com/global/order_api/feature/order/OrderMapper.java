package com.global.order_api.feature.order;

import com.global.order_api.core.base.BaseMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {OrderItemMappper.class})
public interface OrderMapper extends BaseMapper<OrderEntity, OrderRequestDto, OrderResponseDto> {

    @Override
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    OrderEntity mapToEntity(OrderRequestDto dto);

    @Override
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.email", target = "customerEmail")
    OrderResponseDto mapToDto(OrderEntity entity);

}
