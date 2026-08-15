package com.aurora.commerce.aftersale;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface AfterSaleRepository extends JpaRepository<AfterSale, Long> {
    Optional<AfterSale> findByOrderNoAndUserId(String orderNo, Long userId);
    Optional<AfterSale> findByAfterSaleNoAndUserId(String afterSaleNo, Long userId);
    List<AfterSale> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<AfterSale> findAllByOrderByCreatedAtDesc();
    List<AfterSale> findByStatusOrderByCreatedAtDesc(AfterSale.Status status);
    long countByStatus(AfterSale.Status status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select afterSale from AfterSale afterSale where afterSale.afterSaleNo = :afterSaleNo")
    Optional<AfterSale> findByAfterSaleNoForUpdate(@Param("afterSaleNo") String afterSaleNo);
}

interface AfterSaleLogRepository extends JpaRepository<AfterSaleLog, Long> {
    List<AfterSaleLog> findByAfterSaleIdOrderByCreatedAtAsc(Long afterSaleId);
}

interface RefundRecordRepository extends JpaRepository<RefundRecord, Long> {
    Optional<RefundRecord> findByAfterSaleId(Long afterSaleId);
}
