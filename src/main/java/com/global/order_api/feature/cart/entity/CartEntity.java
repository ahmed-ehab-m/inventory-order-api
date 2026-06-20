package com.global.order_api.feature.cart.entity;

import com.global.order_api.core.base.BaseEntity;
import com.global.order_api.feature.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cart")
@Getter // (Serialization)for jackson to convert object to json using get to get values to put it into json
// and mapstruct use it get , set for map , unmap
@NoArgsConstructor // jpa need it to create entity from db+ get ,set to get values or edit fields
// Desrialization jackson use it to create empty obj first
@Setter // then use setter to put values into obj
// jackson may use allargs constructor but need more settings
@AllArgsConstructor // for builder
@Builder
public class CartEntity extends BaseEntity<Long> {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /// to make bidirectional relationship
    /// mappedBy => tell hibernate that => the FK not here
    /// in CartItemEntity => field called "cart"
    /// to prevent jpa to create a new table
    ///
    /// CascadeType.ALL => following
    /// if i save the cart , cart items will be saved
    /// if i delete the cart , cart items will be removed
    ///
    /// orphanRemoval => if i delete cartItem from list items
    /// hibernate will remove it from DB
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    /// initialize => to avoid NullPointerException when adding new cartItem
    private List<CartItemEntity> items = new ArrayList<>();

    /// to fix issue => cart_id can't be Null
    public void addCartItem(CartItemEntity item) {
        this.items.add(item);
        item.setCart(this);
    }
}
