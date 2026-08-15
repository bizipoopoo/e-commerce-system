package com.aurora.commerce.catalog;

import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
class CatalogService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final ProductSkuRepository skuRepository;
    private final BannerRepository bannerRepository;

    CatalogService(
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            ProductRepository productRepository,
            ProductSkuRepository skuRepository,
            BannerRepository bannerRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.skuRepository = skuRepository;
        this.bannerRepository = bannerRepository;
    }

    @Transactional(readOnly = true)
    List<CategoryView> categories() {
        return categoryRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
                .map(category -> new CategoryView(
                        category.id(), category.parentId(), category.name(), category.slug(), category.icon()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    ProductPage products(String keyword, Long categoryId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String searchKeyword = keyword == null ? "" : keyword.trim();
        Page<Product> result = categoryId == null
                ? productRepository.findByStatusAndNameContainingIgnoreCase(
                        ProductStatus.PUBLISHED, searchKeyword, pageable)
                : productRepository.findByStatusAndCategoryIdAndNameContainingIgnoreCase(
                        ProductStatus.PUBLISHED, categoryId, searchKeyword, pageable);
        List<ProductCard> items = toProductCards(result.getContent());
        return new ProductPage(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    ProductDetail product(Long id) {
        Product product = publishedProduct(id);
        Category category = categoryRepository.findById(product.categoryId()).orElseThrow();
        Brand brand = brandRepository.findById(product.brandId()).orElseThrow();
        List<SkuView> skus = skuRepository.findByProductIdAndStatusOrderBySalePriceAsc(id, SkuStatus.ACTIVE)
                .stream().map(this::toSkuView).toList();
        return new ProductDetail(
                product.id(), product.status().name(), product.name(), product.subtitle(), product.description(), product.coverImageUrl(),
                product.rating(), product.salesCount(),
                new CategoryView(category.id(), category.parentId(), category.name(), category.slug(), category.icon()),
                new BrandView(brand.id(), brand.name()),
                skus
        );
    }

    @Transactional(readOnly = true)
    HomeView home() {
        List<BannerView> heroBanners = bannerRepository
                .findByPositionCodeAndEnabledTrueOrderBySortOrderAsc("HOME_HERO")
                .stream().map(this::toBannerView).toList();
        List<BannerView> featureBanners = bannerRepository
                .findByPositionCodeAndEnabledTrueOrderBySortOrderAsc("HOME_FEATURE")
                .stream().map(this::toBannerView).toList();
        return new HomeView(
                heroBanners,
                featureBanners,
                categories(),
                toProductCards(productRepository.findTop8ByStatusAndFeaturedTrueOrderBySalesCountDesc(
                        ProductStatus.PUBLISHED)),
                toProductCards(productRepository.findTop8ByStatusOrderByCreatedAtDesc(ProductStatus.PUBLISHED))
        );
    }

    @Transactional
    ProductDetail createProduct(CreateProductCommand command) {
        requireCategoryAndBrand(command.categoryId(), command.brandId());
        validateSkus(command.skus());
        Product product = productRepository.save(new Product(
                command.categoryId(), command.brandId(), command.name().trim(), command.subtitle(),
                command.description(), command.coverImageUrl(), command.featured()
        ));
        List<ProductSku> skus = command.skus().stream()
                .map(sku -> new ProductSku(
                        product.id(), sku.skuCode().trim().toUpperCase(Locale.ROOT), sku.name().trim(), sku.specValues(),
                        sku.salePrice(), sku.marketPrice()
                ))
                .toList();
        skuRepository.saveAll(skus);
        return adminProduct(product.id());
    }

    @Transactional
    ProductDetail updateProduct(Long id, UpdateProductCommand command) {
        requireCategoryAndBrand(command.categoryId(), command.brandId());
        Product product = productRepository.findById(id).orElseThrow(() -> productNotFound(id));
        product.update(
                command.categoryId(), command.brandId(), command.name().trim(), command.subtitle(),
                command.description(), command.coverImageUrl(), command.featured()
        );
        return adminProduct(id);
    }

    @Transactional
    void publish(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> productNotFound(id));
        if (skuRepository.findByProductIdAndStatusOrderBySalePriceAsc(id, SkuStatus.ACTIVE).isEmpty()) {
            throw new BusinessException("PRODUCT_HAS_NO_ACTIVE_SKU", "商品没有可用 SKU，不能上架", HttpStatus.CONFLICT);
        }
        product.publish();
    }

    @Transactional
    void archive(Long id) {
        productRepository.findById(id).orElseThrow(() -> productNotFound(id)).archive();
    }

    private ProductDetail adminProduct(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> productNotFound(id));
        Category category = categoryRepository.findById(product.categoryId()).orElseThrow();
        Brand brand = brandRepository.findById(product.brandId()).orElseThrow();
        List<SkuView> skus = skuRepository.findByProductIdAndStatusOrderBySalePriceAsc(id, SkuStatus.ACTIVE)
                .stream().map(this::toSkuView).toList();
        return new ProductDetail(
                product.id(), product.status().name(), product.name(), product.subtitle(), product.description(), product.coverImageUrl(),
                product.rating(), product.salesCount(),
                new CategoryView(category.id(), category.parentId(), category.name(), category.slug(), category.icon()),
                new BrandView(brand.id(), brand.name()), skus
        );
    }

    private List<ProductCard> toProductCards(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Long, Category> categories = categoryRepository.findAllById(
                        products.stream().map(Product::categoryId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Category::id, Function.identity()));
        Map<Long, Brand> brands = brandRepository.findAllById(
                        products.stream().map(Product::brandId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(Brand::id, Function.identity()));
        Map<Long, List<ProductSku>> skus = skuRepository.findByProductIdInAndStatus(
                        products.stream().map(Product::id).toList(), SkuStatus.ACTIVE)
                .stream().collect(Collectors.groupingBy(ProductSku::productId));
        return products.stream().map(product -> {
            List<ProductSku> productSkus = skus.getOrDefault(product.id(), List.of());
            BigDecimal salePrice = minPrice(productSkus, ProductSku::salePrice);
            BigDecimal marketPrice = minPrice(productSkus, ProductSku::marketPrice);
            Category category = categories.get(product.categoryId());
            Brand brand = brands.get(product.brandId());
            return new ProductCard(
                    product.id(), product.name(), product.subtitle(), product.coverImageUrl(),
                    salePrice, marketPrice, product.rating(), product.salesCount(),
                    category == null ? null : category.name(), brand == null ? null : brand.name()
            );
        }).toList();
    }

    private BigDecimal minPrice(List<ProductSku> skus, Function<ProductSku, BigDecimal> extractor) {
        return skus.stream().map(extractor).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
    }

    private SkuView toSkuView(ProductSku sku) {
        return new SkuView(
                sku.id(), sku.skuCode(), sku.name(), sku.specValues(), sku.salePrice(), sku.marketPrice()
        );
    }

    private BannerView toBannerView(Banner banner) {
        return new BannerView(banner.id(), banner.title(), banner.subtitle(), banner.imageUrl(), banner.linkUrl());
    }

    private Product publishedProduct(Long id) {
        Product product = productRepository.findById(id).orElseThrow(() -> productNotFound(id));
        if (product.status() != ProductStatus.PUBLISHED) {
            throw productNotFound(id);
        }
        return product;
    }

    private void requireCategoryAndBrand(Long categoryId, Long brandId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new BusinessException("CATEGORY_NOT_FOUND", "商品类目不存在", HttpStatus.BAD_REQUEST);
        }
        if (!brandRepository.existsById(brandId)) {
            throw new BusinessException("BRAND_NOT_FOUND", "商品品牌不存在", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateSkus(List<SkuCommand> skus) {
        Set<String> skuCodes = new HashSet<>();
        for (SkuCommand sku : skus) {
            String normalizedCode = sku.skuCode().trim().toUpperCase(Locale.ROOT);
            if (!skuCodes.add(normalizedCode)) {
                throw new BusinessException("DUPLICATE_SKU_CODE", "同一商品内 SKU 编码不能重复", HttpStatus.BAD_REQUEST);
            }
            if (sku.marketPrice().compareTo(sku.salePrice()) < 0) {
                throw new BusinessException(
                        "INVALID_SKU_PRICE",
                        "SKU 市场价不能低于销售价",
                        HttpStatus.BAD_REQUEST
                );
            }
        }
    }

    private BusinessException productNotFound(Long id) {
        return new BusinessException("PRODUCT_NOT_FOUND", "商品不存在: " + id, HttpStatus.NOT_FOUND);
    }

    record CategoryView(Long id, Long parentId, String name, String slug, String icon) {
    }

    record BrandView(Long id, String name) {
    }

    record ProductCard(
            Long id,
            String name,
            String subtitle,
            String coverImageUrl,
            BigDecimal salePrice,
            BigDecimal marketPrice,
            BigDecimal rating,
            long salesCount,
            String categoryName,
            String brandName
    ) {
    }

    record ProductPage(List<ProductCard> items, int page, int size, long totalElements, int totalPages) {
    }

    record SkuView(
            Long id,
            String skuCode,
            String name,
            String specValues,
            BigDecimal salePrice,
            BigDecimal marketPrice
    ) {
    }

    record ProductDetail(
            Long id,
            String status,
            String name,
            String subtitle,
            String description,
            String coverImageUrl,
            BigDecimal rating,
            long salesCount,
            CategoryView category,
            BrandView brand,
            List<SkuView> skus
    ) {
    }

    record BannerView(Long id, String title, String subtitle, String imageUrl, String linkUrl) {
    }

    record HomeView(
            List<BannerView> heroBanners,
            List<BannerView> featureBanners,
            List<CategoryView> categories,
            List<ProductCard> featuredProducts,
            List<ProductCard> newArrivals
    ) {
    }

    record SkuCommand(
            String skuCode,
            String name,
            String specValues,
            BigDecimal salePrice,
            BigDecimal marketPrice
    ) {
    }

    record CreateProductCommand(
            Long categoryId,
            Long brandId,
            String name,
            String subtitle,
            String description,
            String coverImageUrl,
            boolean featured,
            List<SkuCommand> skus
    ) {
    }

    record UpdateProductCommand(
            Long categoryId,
            Long brandId,
            String name,
            String subtitle,
            String description,
            String coverImageUrl,
            boolean featured
    ) {
    }
}
