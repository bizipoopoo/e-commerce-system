package com.aurora.commerce.engagement;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ProductFavoriteRepository extends JpaRepository<ProductFavorite, Long> {
    Optional<ProductFavorite> findByUserIdAndProductId(Long userId, Long productId);
    List<ProductFavorite> findByUserIdOrderByCreatedAtDesc(Long userId);
    long deleteByUserIdAndProductId(Long userId, Long productId);
}

interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    Optional<ProductReview> findByOrderItemId(Long orderItemId);
    Optional<ProductReview> findByIdAndUserId(Long id, Long userId);
    List<ProductReview> findByProductIdAndStatusOrderByCreatedAtDesc(
            Long productId, ProductReview.Status status);
    List<ProductReview> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<ProductReview> findAllByOrderByCreatedAtDesc();
    List<ProductReview> findByStatusOrderByCreatedAtDesc(ProductReview.Status status);
}
