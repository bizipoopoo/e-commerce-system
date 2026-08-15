package com.aurora.commerce.cart;

import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class CartController {

    private final CartService cartService;

    CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    ApiResponse<CartService.CartView> cart(JwtAuthenticationToken authentication) {
        return ApiResponse.success(cartService.cart(userId(authentication)));
    }

    @PostMapping("/cart/items")
    ApiResponse<CartService.CartView> add(
            JwtAuthenticationToken authentication,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return ApiResponse.success(cartService.add(userId(authentication), request.skuId(), request.quantity()));
    }

    @PatchMapping("/cart/items/{itemId}")
    ApiResponse<CartService.CartView> update(
            JwtAuthenticationToken authentication,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return ApiResponse.success(cartService.update(
                userId(authentication), itemId, request.quantity(), request.selected()
        ));
    }

    @DeleteMapping("/cart/items/{itemId}")
    ApiResponse<CartService.CartView> remove(
            JwtAuthenticationToken authentication,
            @PathVariable Long itemId
    ) {
        return ApiResponse.success(cartService.remove(userId(authentication), itemId));
    }

    @PostMapping("/checkout/preview")
    ApiResponse<CartService.CheckoutPreview> checkoutPreview(JwtAuthenticationToken authentication) {
        return ApiResponse.success(cartService.checkoutPreview(userId(authentication)));
    }

    private Long userId(JwtAuthenticationToken authentication) {
        Number userId = authentication.getToken().getClaim("userId");
        return userId.longValue();
    }

    record AddCartItemRequest(
            @NotNull Long skuId,
            @Min(1) @Max(99) int quantity
    ) {
    }

    record UpdateCartItemRequest(
            @Min(1) @Max(99) Integer quantity,
            Boolean selected
    ) {
    }
}
