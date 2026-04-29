package com.global.order_api.feature.order;

import com.global.order_api.core.base.SoftDeletableEntity;
import com.global.order_api.feature.user.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "orders")
@Builder
@SQLDelete(sql = "UPDATE users SET is_deleted=true WHERE id=?")
@SQLRestriction("is_deleted=false") // when use find all excluding deleted users
public class OrderEntity extends SoftDeletableEntity<Long> {

    @Enumerated(EnumType.STRING)
    @Column(name = "status",nullable = false,length = 50)
    private OrderStatus status;

    @Column(name = "total_price",nullable = false)
    private BigDecimal totalPrice;

    @Column(name = "shipping_address",length = 500)
    private String shippingAddress;

    @Column(name = "order_notes",nullable = true,length = 500)
    private String orderNotes;

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
