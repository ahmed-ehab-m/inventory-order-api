package com.global.order_api.feature.cart;

import com.global.order_api.core.base.BaseRepo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartItemRepo extends BaseRepo<CartItemEntity,Long> {

    /// check if product already exists in cart
    /// to increase quantity only
    Optional<CartItemEntity> findByCartIdAndProductId(Long cartId,Long productId);
}
