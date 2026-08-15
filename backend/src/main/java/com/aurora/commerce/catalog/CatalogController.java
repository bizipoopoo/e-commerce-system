package com.aurora.commerce.catalog;

import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1")
class CatalogController {

    private final CatalogService catalogService;

    CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/home")
    ApiResponse<CatalogService.HomeView> home() {
        return ApiResponse.success(catalogService.home());
    }

    @GetMapping("/categories")
    ApiResponse<List<CatalogService.CategoryView>> categories() {
        return ApiResponse.success(catalogService.categories());
    }

    @GetMapping("/products")
    ApiResponse<CatalogService.ProductPage> products(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ApiResponse.success(catalogService.products(keyword, categoryId, page, size));
    }

    @GetMapping("/products/{id}")
    ApiResponse<CatalogService.ProductDetail> product(@PathVariable Long id) {
        return ApiResponse.success(catalogService.product(id));
    }
}
