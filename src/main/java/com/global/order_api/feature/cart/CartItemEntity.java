package com.global.order_api.feature.cart;

import com.global.order_api.core.base.BaseEntity;
import com.global.order_api.feature.product.ProductEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cart_items")
@Getter // (Serialization)for jackson to convert object to json using get to get values to put it into json
// and mapstruct use it get , set for map , unmap
@NoArgsConstructor // jpa need it to create entity from db+ get ,set to get values or edit fields
// Desrialization jackson use it to create empty obj first
@Setter // then use setter to put values into obj
// jackson may use allargs constructor but need more settings
@AllArgsConstructor // for builder
@Builder

public class CartItemEntity extends BaseEntity<Long> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id",nullable = false)
    private CartEntity cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id",nullable = false)
    private ProductEntity product;

    @Column(name = "quantity",nullable = false)
    private Integer quantity;
}
