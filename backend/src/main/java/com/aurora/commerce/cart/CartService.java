package com.aurora.commerce.cart;

import com.aurora.commerce.catalog.CatalogFacade;
import com.aurora.commerce.inventory.InventoryFacade;
import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
class CartService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("299.00");
    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("15.00");

    private final CartItemRepository cartRepository;
    private final CatalogFacade catalogFacade;
    private final InventoryFacade inventoryFacade;

    CartService(
            CartItemRepository cartRepository,
            CatalogFacade catalogFacade,
            InventoryFacade inventoryFacade
    ) {
        this.cartRepository = cartRepository;
        this.catalogFacade = catalogFacade;
        this.inventoryFacade = inventoryFacade;
    }

    @Transactional(readOnly = true)
    CartView cart(Long userId) {
        return enrich(cartRepository.findByUserIdOrderByUpdatedAtDesc(userId));
    }

    @Transactional
    CartView add(Long userId, Long skuId, int quantity) {
        if (quantity < 1 || quantity > 99) {
            throw invalidQuantity();
        }
        catalogFacade.purchasableSku(skuId);
        InventoryFacade.StockView stock = inventoryFacade.stock(skuId);
        CartItem item = cartRepository.findByUserIdAndSkuId(userId, skuId)
                .orElseGet(() -> new CartItem(userId, skuId, 0));
        item.increase(quantity, stock.availableQuantity());
        cartRepository.save(item);
        return cart(userId);
    }

    @Transactional
    CartView update(Long userId, Long itemId, Integer quantity, Boolean selected) {
        CartItem item = ownedItem(userId, itemId);
        if (quantity == null && selected == null) {
            throw new BusinessException("EMPTY_CART_UPDATE", "没有需要更新的购物车字段", HttpStatus.BAD_REQUEST);
        }
        if (quantity != null) {
            catalogFacade.purchasableSku(item.skuId());
            item.setQuantity(quantity, inventoryFacade.stock(item.skuId()).availableQuantity());
        }
        if (selected != null) {
            item.setSelected(selected);
        }
        return cart(userId);
    }

    @Transactional
    CartView remove(Long userId, Long itemId) {
        if (cartRepository.deleteByIdAndUserId(itemId, userId) == 0) {
            throw cartItemNotFound();
        }
        return cart(userId);
    }

    @Transactional(readOnly = true)
    CheckoutPreview checkoutPreview(Long userId) {
        List<CartItem> selectedItems = cartRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream().filter(CartItem::selected).toList();
        if (selectedItems.isEmpty()) {
            throw new BusinessException("EMPTY_CHECKOUT", "请至少勾选一件商品", HttpStatus.BAD_REQUEST);
        }

        Map<Long, CatalogFacade.PurchasableSku> skus = catalogFacade.purchasableSkus(
                selectedItems.stream().map(CartItem::skuId).collect(Collectors.toSet()));
        if (skus.size() != selectedItems.stream().map(CartItem::skuId).distinct().count()) {
            throw new BusinessException("CART_CONTAINS_INVALID_ITEM", "购物车中包含失效商品", HttpStatus.CONFLICT);
        }
        Map<Long, Integer> quantities = selectedItems.stream().collect(Collectors.toMap(
                CartItem::skuId, CartItem::quantity, Integer::sum));
        inventoryFacade.ensureAvailable(quantities);

        List<CheckoutItem> items = selectedItems.stream().map(item -> {
            CatalogFacade.PurchasableSku sku = skus.get(item.skuId());
            BigDecimal subtotal = money(sku.salePrice().multiply(BigDecimal.valueOf(item.quantity())));
            return new CheckoutItem(
                    item.id(), sku.skuId(), sku.productId(), sku.productName(), sku.skuName(),
                    sku.imageUrl(), sku.salePrice(), item.quantity(), subtotal
            );
        }).toList();
        BigDecimal goodsAmount = money(items.stream()
                .map(CheckoutItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal shippingAmount = goodsAmount.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO.setScale(2)
                : STANDARD_SHIPPING_FEE;
        BigDecimal payableAmount = money(goodsAmount.add(shippingAmount));
        return new CheckoutPreview(items, goodsAmount, BigDecimal.ZERO.setScale(2), shippingAmount, payableAmount);
    }

    private CartView enrich(List<CartItem> cartItems) {
        Map<Long, CatalogFacade.PurchasableSku> skus = catalogFacade.purchasableSkus(
                cartItems.stream().map(CartItem::skuId).collect(Collectors.toSet()));
        Map<Long, InventoryFacade.StockView> stocks = inventoryFacade.stocks(
                cartItems.stream().map(CartItem::skuId).collect(Collectors.toSet()));
        List<CartItemView> items = cartItems.stream().map(item -> {
            CatalogFacade.PurchasableSku sku = skus.get(item.skuId());
            InventoryFacade.StockView stock = stocks.get(item.skuId());
            boolean available = sku != null && stock != null && stock.availableQuantity() >= item.quantity();
            BigDecimal unitPrice = sku == null ? BigDecimal.ZERO.setScale(2) : sku.salePrice();
            return new CartItemView(
                    item.id(), item.skuId(), sku == null ? null : sku.productId(),
                    sku == null ? "商品已失效" : sku.productName(),
                    sku == null ? "" : sku.skuName(),
                    sku == null ? "" : sku.imageUrl(),
                    unitPrice, item.quantity(), money(unitPrice.multiply(BigDecimal.valueOf(item.quantity()))),
                    item.selected(), available, stock == null ? 0 : stock.availableQuantity()
            );
        }).toList();
        int selectedQuantity = items.stream()
                .filter(item -> item.selected() && item.available())
                .mapToInt(CartItemView::quantity).sum();
        BigDecimal selectedAmount = money(items.stream()
                .filter(item -> item.selected() && item.available())
                .map(CartItemView::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return new CartView(items, items.stream().mapToInt(CartItemView::quantity).sum(), selectedQuantity, selectedAmount);
    }

    private CartItem ownedItem(Long userId, Long itemId) {
        return cartRepository.findByIdAndUserId(itemId, userId).orElseThrow(this::cartItemNotFound);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BusinessException invalidQuantity() {
        return new BusinessException(
                "INVALID_CART_QUANTITY", "购物车数量必须在 1 到 99 之间", HttpStatus.BAD_REQUEST);
    }

    private BusinessException cartItemNotFound() {
        return new BusinessException("CART_ITEM_NOT_FOUND", "购物车商品不存在", HttpStatus.NOT_FOUND);
    }

    record CartItemView(
            Long id,
            Long skuId,
            Long productId,
            String productName,
            String skuName,
            String imageUrl,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal,
            boolean selected,
            boolean available,
            int availableQuantity
    ) {
    }

    record CartView(
            List<CartItemView> items,
            int totalQuantity,
            int selectedQuantity,
            BigDecimal selectedAmount
    ) {
    }

    record CheckoutItem(
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

    record CheckoutPreview(
            List<CheckoutItem> items,
            BigDecimal goodsAmount,
            BigDecimal discountAmount,
            BigDecimal shippingAmount,
            BigDecimal payableAmount
    ) {
    }
}
