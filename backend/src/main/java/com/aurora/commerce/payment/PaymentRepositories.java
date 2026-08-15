package com.aurora.commerce.payment;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

interface PaymentOrderRepository extends JpaRepository<PaymentOrder, Long> {
    Optional<PaymentOrder> findByOrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from PaymentOrder payment where payment.paymentNo = :paymentNo")
    Optional<PaymentOrder> findByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);
}

interface PaymentCallbackRepository extends JpaRepository<PaymentCallback, Long> {
}
