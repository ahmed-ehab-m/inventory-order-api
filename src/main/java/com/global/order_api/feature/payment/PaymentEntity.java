package com.global.order_api.feature.payment;

import com.global.order_api.feature.order.OrderEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "payments")
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PaymentEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Column(name = "paymob_order_id", length = 255, nullable = false)
    private String paymobOrderId;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;


    @Builder.Default
    @Column(name = "currency", length = 10)
    private String currency = " EGP";

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "provider_response_code", length = 255)
    private String providerResponseCode;

    @Column(name = "provider_message", length = 255)
    private String providerMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 50)
    private PaymentStatus paymentStatus;

    /// string because our system must be robust if payment type come from apis not in our enums
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

}
