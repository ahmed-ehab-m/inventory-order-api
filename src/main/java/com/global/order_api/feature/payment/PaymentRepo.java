package com.global.order_api.feature.payment;

import com.global.order_api.core.base.BaseRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepo extends JpaRepository<PaymentEntity,Long> {

    /// TO UPDATE STATUS WHEN RECEIVED WEBHOOK FROM PAYMOB
    Optional<PaymentEntity> findByTransactionId(String transactionId);


    Optional<PaymentEntity> findByPaymobOrderId(String paymobOrderId);

    List<PaymentEntity> findByOrderId(Long orderId);
}
