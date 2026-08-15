package com.aurora.commerce.catalog;

import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CatalogFacade {

    private final ProductRepository productRepository;
    private final ProductSkuRepository skuRepository;

    CatalogFacade(ProductRepository productRepository, ProductSkuRepository skuRepository) {
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
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
}
