package com.aurora.commerce.cart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<CartItem> findByUserIdAndSkuId(Long userId, Long skuId);

    Optional<CartItem> findByIdAndUserId(Long id, Long userId);

    long deleteByIdAndUserId(Long id, Long userId);
}
