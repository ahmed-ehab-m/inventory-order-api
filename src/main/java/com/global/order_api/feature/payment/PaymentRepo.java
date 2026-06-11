package com.global.order_api.feature.payment;

import com.global.order_api.core.base.BaseRepo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepo extends BaseRepo<PaymentEntity,Long> {

    /// TO UPDATE STATUS WHEN RECEIVED WEBHOOK FROM PAYMOB
    Optional<PaymentEntity> findByTransactionId(String transactionId);

    List<PaymentEntity> findByOrderId(Long orderId);
}
