package com.aurora.commerce.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByEnabledTrueOrderBySortOrderAsc();
}

interface BrandRepository extends JpaRepository<Brand, Long> {
}

interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByStatusAndNameContainingIgnoreCase(
            ProductStatus status,
            String keyword,
            Pageable pageable
    );

    Page<Product> findByStatusAndCategoryIdAndNameContainingIgnoreCase(
            ProductStatus status,
            Long categoryId,
            String keyword,
            Pageable pageable
    );

    List<Product> findTop8ByStatusAndFeaturedTrueOrderBySalesCountDesc(ProductStatus status);

    List<Product> findTop8ByStatusOrderByCreatedAtDesc(ProductStatus status);

    List<Product> findTop20ByStatusOrderBySalesCountDesc(ProductStatus status);
}

interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {
    List<ProductSku> findByIdInAndStatus(Collection<Long> skuIds, SkuStatus status);

    List<ProductSku> findByProductIdInAndStatus(Collection<Long> productIds, SkuStatus status);

    List<ProductSku> findByProductIdAndStatusOrderBySalePriceAsc(Long productId, SkuStatus status);
}

interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findByPositionCodeAndEnabledTrueOrderBySortOrderAsc(String positionCode);
}
