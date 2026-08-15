package com.aurora.commerce.catalog;

import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CatalogFacade {

    private final ProductRepository productRepository;
    private final ProductSkuRepository skuRepository;
    private final CategoryRepository categoryRepository;

    CatalogFacade(
            ProductRepository productRepository,
            ProductSkuRepository skuRepository,
            CategoryRepository categoryRepository
    ) {
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public PurchasableSku purchasableSku(Long skuId) {
        return purchasableSkus(List.of(skuId)).values().stream()
                .findFirst()
                .orElseThrow(() -> unavailableSku(skuId));
    }

    @Transactional(readOnly = true)
    public Map<Long, PurchasableSku> purchasableSkus(Collection<Long> skuIds) {
        if (skuIds.isEmpty()) {
            return Map.of();
        }
        List<ProductSku> skus = skuRepository.findByIdInAndStatus(skuIds, SkuStatus.ACTIVE);
        Map<Long, Product> products = productRepository.findAllById(
                        skus.stream().map(ProductSku::productId).collect(Collectors.toSet()))
                .stream()
                .filter(product -> product.status() == ProductStatus.PUBLISHED)
                .collect(Collectors.toMap(Product::id, Function.identity()));
        return skus.stream()
                .filter(sku -> products.containsKey(sku.productId()))
                .collect(Collectors.toMap(ProductSku::id, sku -> {
                    Product product = products.get(sku.productId());
                    return new PurchasableSku(
                            sku.id(), product.id(), product.name(), sku.name(), product.coverImageUrl(),
                            sku.salePrice(), sku.marketPrice()
                    );
                }));
    }

    @Transactional(readOnly = true)
    public boolean skuExists(Long skuId) {
        return skuRepository.existsById(skuId);
    }

    @Transactional(readOnly = true)
    public List<RecommendationCandidate> recommendationCandidates() {
        List<Product> products = productRepository.findTop20ByStatusOrderBySalesCountDesc(ProductStatus.PUBLISHED);
        Map<Long, List<ProductSku>> skus = skuRepository.findByProductIdInAndStatus(
                        products.stream().map(Product::id).toList(), SkuStatus.ACTIVE).stream()
                .collect(Collectors.groupingBy(ProductSku::productId));
        Map<Long, Category> categories = categoryRepository.findAllById(
                        products.stream().map(Product::categoryId).collect(Collectors.toSet())).stream()
                .collect(Collectors.toMap(Category::id, Function.identity()));
        return products.stream().map(product -> {
            ProductSku defaultSku = skus.getOrDefault(product.id(), List.of()).stream()
                    .min(Comparator.comparing(ProductSku::salePrice)).orElse(null);
            Category category = categories.get(product.categoryId());
            return new RecommendationCandidate(
                    product.id(), defaultSku == null ? null : defaultSku.id(), product.name(),
                    product.subtitle(), product.coverImageUrl(),
                    defaultSku == null ? BigDecimal.ZERO : defaultSku.salePrice(),
                    product.categoryId(), category == null ? "精选" : category.name(),
                    product.featured(), product.salesCount()
            );
        }).filter(candidate -> candidate.defaultSkuId() != null).toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> productCategoryIds(Collection<Long> productIds) {
        Map<Long, Long> result = new LinkedHashMap<>();
        productRepository.findAllById(productIds).forEach(product ->
                result.put(product.id(), product.categoryId()));
        return result;
    }

    private BusinessException unavailableSku(Long skuId) {
        return new BusinessException(
                "SKU_NOT_AVAILABLE",
                "SKU 不存在或当前不可售: " + skuId,
                HttpStatus.CONFLICT
        );
    }

    public record PurchasableSku(
            Long skuId,
            Long productId,
            String productName,
            String skuName,
            String imageUrl,
            BigDecimal salePrice,
            BigDecimal marketPrice
    ) {
    }

    public record RecommendationCandidate(
            Long productId,
            Long defaultSkuId,
            String productName,
            String subtitle,
            String imageUrl,
            BigDecimal salePrice,
            Long categoryId,
            String categoryName,
            boolean featured,
            long salesCount
    ) {
    }
}
