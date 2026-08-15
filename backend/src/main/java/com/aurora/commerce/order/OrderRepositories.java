package com.aurora.commerce.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findByOrderNo(String orderNo);
    Optional<CustomerOrder> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);
    Optional<CustomerOrder> findByOrderNoAndUserId(String orderNo, Long userId);
    List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();
    List<CustomerOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    @Query("""
            select customerOrder.orderNo from CustomerOrder customerOrder
             where customerOrder.status = :status and customerOrder.expireAt < :expireAt
            """)
    List<String> findExpirableOrderNos(
            @Param("status") OrderStatus status,
            @Param("expireAt") Instant expireAt
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select customerOrder from CustomerOrder customerOrder where customerOrder.orderNo = :orderNo")
    Optional<CustomerOrder> findByOrderNoForUpdate(@Param("orderNo") String orderNo);
}

interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderIdOrderByIdAsc(Long orderId);
}

interface OrderStatusLogRepository extends JpaRepository<OrderStatusLog, Long> {
    List<OrderStatusLog> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}
