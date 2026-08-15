package com.aurora.commerce.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CartFacade {

    private final CartService cartService;

    CartFacade(CartService cartService) {
        this.cartService = cartService;
    }

    @Transactional
    public OrderCart selectedForOrder(Long userId) {
        CartService.OrderCart cart = cartService.orderCart(userId);
        return new OrderCart(
                cart.items().stream().map(item -> new OrderCartLine(
                        item.cartItemId(), item.skuId(), item.productId(), item.productName(), item.skuName(),
                        item.imageUrl(), item.unitPrice(), item.quantity(), item.subtotal()
                )).toList(),
                cart.goodsAmount(), cart.discountAmount(), cart.shippingAmount(), cart.payableAmount()
        );
    }

    @Transactional
    public void clearOrderedItems(Long userId, List<Long> itemIds) {
        cartService.clearOrderedItems(userId, itemIds);
    }

    public record OrderCartLine(
            Long cartItemId,
            Long skuId,
            Long productId,
            String productName,
            String skuName,
            String imageUrl,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal
    ) {
    }

    public record OrderCart(
            List<OrderCartLine> items,
            BigDecimal goodsAmount,
            BigDecimal discountAmount,
            BigDecimal shippingAmount,
            BigDecimal payableAmount
    ) {
    }
}
