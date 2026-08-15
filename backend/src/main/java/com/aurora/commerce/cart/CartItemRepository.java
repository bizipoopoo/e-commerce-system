package com.aurora.commerce.cart;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByUpdatedAtDesc(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item from CartItem item
             where item.userId = :userId and item.selected = true
             order by item.updatedAt desc
            """)
    List<CartItem> findSelectedForOrder(@Param("userId") Long userId);

    Optional<CartItem> findByUserIdAndSkuId(Long userId, Long skuId);

    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    long deleteByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("delete from CartItem item where item.userId = :userId and item.id in :itemIds")
    int deleteOwnedItems(@Param("userId") Long userId, @Param("itemIds") List<Long> itemIds);
}
