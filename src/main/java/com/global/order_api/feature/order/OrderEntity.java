package com.global.order_api.feature.order;

import com.global.order_api.core.base.SoftDeletableEntity;
import com.global.order_api.feature.user.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class OrderEntity extends SoftDeletableEntity<Long> {

    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false,length = 50)
    private OrderStatus status;

    @Column(name = "total_price",nullable = false)
    private BigDecimal totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> orderItems=new ArrayList<>();

    public void addOrderItem(OrderItemEntity orderItemEntity)
    {
        orderItems.add(orderItemEntity);
        orderItemEntity.setOrder(this);
    }

}
