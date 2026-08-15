package com.aurora.commerce.catalog;

import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/products")
class AdminCatalogController {

    private final CatalogService catalogService;

    AdminCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    ApiResponse<CatalogService.ProductDetail> create(@Valid @RequestBody CreateProductRequest request) {
        return ApiResponse.success(catalogService.createProduct(request.toCommand()));
    }

    @PutMapping("/{id}")
    ApiResponse<CatalogService.ProductDetail> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return ApiResponse.success(catalogService.updateProduct(id, request.toCommand()));
    }

    @PatchMapping("/{id}/publish")
    ApiResponse<Void> publish(@PathVariable Long id) {
        catalogService.publish(id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/{id}/archive")
    ApiResponse<Void> archive(@PathVariable Long id) {
        catalogService.archive(id);
        return ApiResponse.success(null);
    }

    record CreateProductRequest(
            @NotNull Long categoryId,
            @NotNull Long brandId,
            @NotBlank @Size(max = 180) String name,
            @Size(max = 300) String subtitle,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 500) String coverImageUrl,
            boolean featured,
            @NotEmpty List<@Valid SkuRequest> skus
    ) {
        CatalogService.CreateProductCommand toCommand() {
            return new CatalogService.CreateProductCommand(
                    categoryId, brandId, name, subtitle, description, coverImageUrl, featured,
                    skus.stream().map(SkuRequest::toCommand).toList()
            );
        }
    }

    record UpdateProductRequest(
            @NotNull Long categoryId,
            @NotNull Long brandId,
            @NotBlank @Size(max = 180) String name,
            @Size(max = 300) String subtitle,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 500) String coverImageUrl,
            boolean featured
    ) {
        CatalogService.UpdateProductCommand toCommand() {
            return new CatalogService.UpdateProductCommand(
                    categoryId, brandId, name, subtitle, description, coverImageUrl, featured
            );
        }
    }

    record SkuRequest(
            @NotBlank @Size(max = 80) String skuCode,
            @NotBlank @Size(max = 180) String name,
            @NotBlank @Size(max = 1000) String specValues,
            @NotNull @DecimalMin("0.01") BigDecimal salePrice,
            @NotNull @DecimalMin("0.01") BigDecimal marketPrice
    ) {
        CatalogService.SkuCommand toCommand() {
            return new CatalogService.SkuCommand(
                    skuCode, name, specValues, salePrice, marketPrice
            );
        }
    }
}
