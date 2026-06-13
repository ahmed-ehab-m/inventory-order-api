package com.global.order_api.feature.payment;

import com.global.order_api.core.base.BaseEntity;
import com.global.order_api.feature.order.OrderEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "payments")
@Builder
public class PaymentEntity extends BaseEntity<Long> {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Column(name = "payment_order_id",length = 255,nullable = false)
    private String paymentOrderId;

    @Column(name = "amount",nullable = false)
    private BigDecimal amount;


    @Builder.Default
    @Column(name = "currency",length = 10)
    private String currency=" EGP";

    @Column(name = "transaction_id",length = 255)
    private String transactionId;

    @Column(name = "provider_response_code",length = 255)
    private String providerResponseCode;

    @Column(name = "provider_message",length = 255)
    private String providerMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status",nullable = false,length = 50)
    private PaymentStatus paymentStatus;

    /// string because our system must be robust if payment type come from apis not in our enums
    @Column(name = "payment_method",length = 50)
    private String paymentMethod;

}
