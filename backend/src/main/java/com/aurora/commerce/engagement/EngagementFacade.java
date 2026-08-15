package com.aurora.commerce.engagement;

import com.aurora.commerce.catalog.CatalogFacade;
import com.aurora.commerce.notification.NotificationFacade;
import com.aurora.commerce.order.OrderFacade;
import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
class EngagementFacade {

    private final ProductFavoriteRepository favoriteRepository;
    private final ProductReviewRepository reviewRepository;
    private final CatalogFacade catalogFacade;
    private final OrderFacade orderFacade;
    private final NotificationFacade notificationFacade;

    EngagementFacade(
            ProductFavoriteRepository favoriteRepository,
            ProductReviewRepository reviewRepository,
            CatalogFacade catalogFacade,
            OrderFacade orderFacade,
            NotificationFacade notificationFacade
    ) {
        this.favoriteRepository = favoriteRepository;
        this.reviewRepository = reviewRepository;
        this.catalogFacade = catalogFacade;
        this.orderFacade = orderFacade;
        this.notificationFacade = notificationFacade;
    }

    @Transactional
    FavoriteView favorite(Long userId, Long productId) {
        ProductFavorite existing = favoriteRepository.findByUserIdAndProductId(userId, productId).orElse(null);
        if (existing != null) return favoriteView(existing, catalogFacade.productSummaries(List.of(productId)));
        if (!catalogFacade.productExists(productId)) {
            throw new BusinessException("PRODUCT_NOT_FOUND", "商品不存在", HttpStatus.NOT_FOUND);
        }
        ProductFavorite favorite = favoriteRepository.save(new ProductFavorite(userId, productId));
        return favoriteView(favorite, catalogFacade.productSummaries(List.of(productId)));
    }

    @Transactional
    void removeFavorite(Long userId, Long productId) {
        favoriteRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Transactional(readOnly = true)
    List<FavoriteView> favorites(Long userId) {
        List<ProductFavorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<Long, CatalogFacade.ProductSummary> products = catalogFacade.productSummaries(
                favorites.stream().map(ProductFavorite::productId).toList());
        return favorites.stream().filter(item -> products.containsKey(item.productId()))
                .map(item -> favoriteView(item, products)).toList();
    }

    @Transactional
    ReviewView createReview(Long userId, CreateReviewCommand command) {
        OrderFacade.ReviewOrder order = orderFacade.reviewOrder(userId, command.orderNo());
        OrderFacade.ReviewOrderItem item = order.items().stream()
                .filter(line -> line.orderItemId().equals(command.orderItemId()))
                .findFirst().orElseThrow(() -> new BusinessException(
                        "ORDER_ITEM_NOT_FOUND", "订单商品不存在", HttpStatus.NOT_FOUND));
        ProductReview existing = reviewRepository.findByOrderItemId(command.orderItemId()).orElse(null);
        if (existing != null) {
            if (!existing.userId().equals(userId)) throw reviewNotFound();
            return toView(existing);
        }
        return toView(reviewRepository.save(new ProductReview(
                userId, order.orderId(), item.orderItemId(), item.productId(), command.rating(),
                command.content(), command.imageUrls())));
    }

    @Transactional(readOnly = true)
    List<ReviewView> productReviews(Long productId) {
        return reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(
                productId, ProductReview.Status.PUBLISHED).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    List<ReviewView> myReviews(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    List<ReviewView> adminReviews(String status) {
        List<ProductReview> reviews;
        if (status == null || status.isBlank()) {
            reviews = reviewRepository.findAllByOrderByCreatedAtDesc();
        } else {
            try {
                reviews = reviewRepository.findByStatusOrderByCreatedAtDesc(
                        ProductReview.Status.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException exception) {
                throw new BusinessException("INVALID_REVIEW_STATUS", "评价状态无效", HttpStatus.BAD_REQUEST);
            }
        }
        return reviews.stream().map(this::toView).toList();
    }

    @Transactional
    ReviewView reply(Long reviewId, String reply) {
        ProductReview review = reviewRepository.findById(reviewId).orElseThrow(this::reviewNotFound);
        review.reply(reply, Instant.now());
        notificationFacade.notifyUser(
                review.userId(), "REVIEW", "商家回复了你的评价", reply,
                "REVIEW", review.id().toString());
        return toView(review);
    }

    @Transactional
    ReviewView hide(Long reviewId) {
        ProductReview review = reviewRepository.findById(reviewId).orElseThrow(this::reviewNotFound);
        review.hide();
        return toView(review);
    }

    private FavoriteView favoriteView(
            ProductFavorite favorite, Map<Long, CatalogFacade.ProductSummary> products
    ) {
        CatalogFacade.ProductSummary product = products.get(favorite.productId());
        if (product == null) {
            return new FavoriteView(favorite.productId(), null, "已下架商品", "", null, favorite.createdAt());
        }
        return new FavoriteView(
                product.productId(), product.defaultSkuId(), product.productName(), product.imageUrl(),
                product.salePrice(), favorite.createdAt());
    }

    private ReviewView toView(ProductReview review) {
        return new ReviewView(
                review.id(), review.userId(), review.orderItemId(), review.productId(), review.rating(),
                review.contentText(), review.imageUrls(), review.status().name(), review.adminReply(),
                review.repliedAt(), review.createdAt());
    }

    private BusinessException reviewNotFound() {
        return new BusinessException("REVIEW_NOT_FOUND", "评价不存在", HttpStatus.NOT_FOUND);
    }

    record CreateReviewCommand(
            String orderNo, Long orderItemId, int rating, String content, String imageUrls
    ) {
    }
    record FavoriteView(
            Long productId, Long defaultSkuId, String productName, String imageUrl,
            java.math.BigDecimal salePrice, Instant createdAt
    ) {
    }
    record ReviewView(
            Long id, Long userId, Long orderItemId, Long productId, int rating, String content,
            String imageUrls, String status, String adminReply, Instant repliedAt, Instant createdAt
    ) {
    }
}
