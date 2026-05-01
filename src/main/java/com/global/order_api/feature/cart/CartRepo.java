package com.global.order_api.feature.cart;

import com.global.order_api.core.base.BaseRepo;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepo extends BaseRepo<CartEntity,Long> {

    /// Optional => if user doesn't have cart
    /// to return the cart of user
    Optional<CartEntity> findByUserId(Long userId);
}
