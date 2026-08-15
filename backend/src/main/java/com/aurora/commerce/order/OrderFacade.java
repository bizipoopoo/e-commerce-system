package com.aurora.commerce.order;

import com.aurora.commerce.cart.CartFacade;
import com.aurora.commerce.inventory.InventoryFacade;
import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderFacade {

    private static final DateTimeFormatter NUMBER_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final CustomerOrderRepository orderRepository;
    private final OrderItemRepository itemRepository;
    private final OrderStatusLogRepository logRepository;
    private final CartFacade cartFacade;
    private final InventoryFacade inventoryFacade;
    private final Clock clock = Clock.systemUTC();

    OrderFacade(
            CustomerOrderRepository orderRepository,
            OrderItemRepository itemRepository,
            OrderStatusLogRepository logRepository,
            CartFacade cartFacade,
            InventoryFacade inventoryFacade
    ) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.logRepository = logRepository;
        this.cartFacade = cartFacade;
        this.inventoryFacade = inventoryFacade;
    }

    @Transactional
    public OrderView create(Long userId, String idempotencyKey, CreateOrderCommand command) {
        validateIdempotencyKey(idempotencyKey);
        CustomerOrder existing = orderRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            return toView(existing);
        }

        CartFacade.OrderCart cart = cartFacade.selectedForOrder(userId);
        String orderNo = nextNumber("ORD");
        Map<Long, Integer> quantities = cart.items().stream().collect(Collectors.toMap(
                CartFacade.OrderCartLine::skuId, CartFacade.OrderCartLine::quantity, Integer::sum));
        inventoryFacade.reserve(reservationKey(orderNo), quantities);

        Instant now = clock.instant();
        CustomerOrder order = orderRepository.save(new CustomerOrder(
                orderNo, userId, idempotencyKey, cart.goodsAmount(), cart.discountAmount(),
                cart.shippingAmount(), cart.payableAmount(), command.receiverName(), command.receiverPhone(),
                command.addressLine(), command.customerNote(), now.plusSeconds(30 * 60)
        ));
        itemRepository.saveAll(cart.items().stream().map(item -> new OrderItem(
                order.id(), item.productId(), item.skuId(), item.productName(), item.skuName(),
                item.imageUrl(), item.unitPrice(), item.quantity(), item.subtotal()
        )).toList());
        logRepository.save(new OrderStatusLog(
                order.id(), null, OrderStatus.PENDING_PAYMENT, "CUSTOMER", "提交订单", now));
        cartFacade.clearOrderedItems(
                userId, cart.items().stream().map(CartFacade.OrderCartLine::cartItemId).toList());
        return toView(order);
    }

    @Transactional(readOnly = true)
    public List<OrderView> list(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public OrderView detail(Long userId, String orderNo) {
        return toView(ownedOrder(userId, orderNo));
    }

    @Transactional(readOnly = true)
    public List<OrderView> adminList(String status) {
        if (status == null || status.isBlank()) {
            return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toView).toList();
        }
        try {
            return orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.valueOf(status.toUpperCase()))
                    .stream().map(this::toView).toList();
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("INVALID_ORDER_STATUS", "订单状态无效", HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional
    public OrderView cancel(Long userId, String orderNo) {
        CustomerOrder order = lockedOrder(orderNo);
        requireOwner(order, userId);
        if (order.status() == OrderStatus.CLOSED) {
            return toView(order);
        }
        OrderStatus from = order.status();
        order.close(clock.instant());
        inventoryFacade.release(reservationKey(orderNo));
        log(order, from, "CUSTOMER", "用户取消订单");
        return toView(order);
    }

    @Transactional
    public OrderView confirmReceipt(Long userId, String orderNo) {
        CustomerOrder order = lockedOrder(orderNo);
        requireOwner(order, userId);
        if (order.status() == OrderStatus.COMPLETED) {
            return toView(order);
        }
        OrderStatus from = order.status();
        order.complete(clock.instant());
        log(order, from, "CUSTOMER", "用户确认收货");
        return toView(order);
    }

    @Transactional(readOnly = true)
    public PaymentTarget paymentTarget(Long userId, String orderNo) {
        CustomerOrder order = ownedOrder(userId, orderNo);
        return new PaymentTarget(
                order.id(), order.orderNo(), order.status().name(), order.payableAmount(), order.expireAt());
    }

    @Transactional(readOnly = true)
    public OrderReference reference(String orderNo) {
        CustomerOrder order = orderRepository.findByOrderNo(orderNo).orElseThrow(this::orderNotFound);
        return new OrderReference(order.id(), order.orderNo(), order.status().name());
    }

    @Transactional
    public OrderView markPaid(String orderNo, BigDecimal amount) {
        CustomerOrder order = lockedOrder(orderNo);
        if (order.status() == OrderStatus.PAID || order.status() == OrderStatus.SHIPPED
                || order.status() == OrderStatus.COMPLETED) {
            return toView(order);
        }
        if (order.status() != OrderStatus.PENDING_PAYMENT || order.payableAmount().compareTo(amount) != 0) {
            throw stateConflict();
        }
        OrderStatus from = order.status();
        inventoryFacade.confirm(reservationKey(orderNo));
        order.markPaid(clock.instant());
        log(order, from, "PAYMENT", "模拟支付成功");
        return toView(order);
    }

    @Transactional
    public OrderView markShipped(String orderNo, String remark) {
        CustomerOrder order = lockedOrder(orderNo);
        if (order.status() == OrderStatus.SHIPPED || order.status() == OrderStatus.COMPLETED) {
            return toView(order);
        }
        OrderStatus from = order.status();
        order.ship(clock.instant());
        log(order, from, "ADMIN", remark);
        return toView(order);
    }

    @Transactional
    public int expirePendingOrders(Instant cutoff) {
        List<String> candidates = orderRepository.findExpirableOrderNos(
                OrderStatus.PENDING_PAYMENT, cutoff);
        int expired = 0;
        for (String orderNo : candidates) {
            CustomerOrder order = lockedOrder(orderNo);
            if (order.status() != OrderStatus.PENDING_PAYMENT || !order.expireAt().isBefore(cutoff)) {
                continue;
            }
            order.close(cutoff);
            inventoryFacade.release(reservationKey(order.orderNo()));
            log(order, OrderStatus.PENDING_PAYMENT, "SYSTEM", "支付超时自动关闭");
            expired++;
        }
        return expired;
    }

    private void log(CustomerOrder order, OrderStatus from, String operator, String remark) {
        logRepository.save(new OrderStatusLog(
                order.id(), from, order.status(), operator, remark, clock.instant()));
    }

    private OrderView toView(CustomerOrder order) {
        List<OrderItemView> items = itemRepository.findByOrderIdOrderByIdAsc(order.id()).stream()
                .map(item -> new OrderItemView(
                        item.id(), item.productId(), item.skuId(), item.productName(), item.skuName(),
                        item.imageUrl(), item.unitPrice(), item.quantity(), item.discountAmount(), item.payableAmount()
                )).toList();
        List<StatusLogView> timeline = logRepository.findByOrderIdOrderByCreatedAtAsc(order.id()).stream()
                .map(log -> new StatusLogView(
                        log.fromStatus() == null ? null : log.fromStatus().name(), log.toStatus().name(),
                        log.operatorType(), log.remark(), log.createdAt()
                )).toList();
        return new OrderView(
                order.orderNo(), order.status().name(), items, order.goodsAmount(), order.discountAmount(),
                order.shippingAmount(), order.payableAmount(), order.receiverName(), order.receiverPhone(),
                order.addressLine(), order.customerNote(), order.expireAt(), order.paidAt(), order.shippedAt(),
                order.completedAt(), order.closedAt(), order.createdAt(), timeline
        );
    }

    private CustomerOrder ownedOrder(Long userId, String orderNo) {
        return orderRepository.findByOrderNoAndUserId(orderNo, userId).orElseThrow(this::orderNotFound);
    }

    private CustomerOrder lockedOrder(String orderNo) {
        return orderRepository.findByOrderNoForUpdate(orderNo).orElseThrow(this::orderNotFound);
    }

    private void requireOwner(CustomerOrder order, Long userId) {
        if (!order.userId().equals(userId)) {
            throw orderNotFound();
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 100) {
            throw new BusinessException(
                    "INVALID_IDEMPOTENCY_KEY", "Idempotency-Key 不能为空且不能超过 100 个字符",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private String nextNumber(String prefix) {
        return prefix + NUMBER_TIME.format(clock.instant())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private String reservationKey(String orderNo) {
        return "ORDER:" + orderNo;
    }

    private BusinessException orderNotFound() {
        return new BusinessException("ORDER_NOT_FOUND", "订单不存在", HttpStatus.NOT_FOUND);
    }

    private BusinessException stateConflict() {
        return new BusinessException(
                "ORDER_STATE_CONFLICT", "当前订单状态不允许此操作", HttpStatus.CONFLICT);
    }

    public record CreateOrderCommand(
            String receiverName,
            String receiverPhone,
            String addressLine,
            String customerNote
    ) {
    }

    public record PaymentTarget(
            Long orderId, String orderNo, String status, BigDecimal amount, Instant expireAt
    ) {
    }

    public record OrderReference(Long orderId, String orderNo, String status) {
    }

    public record OrderItemView(
            Long id,
            Long productId,
            Long skuId,
            String productName,
            String skuName,
            String imageUrl,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal discountAmount,
            BigDecimal payableAmount
    ) {
    }

    public record StatusLogView(
            String fromStatus,
            String toStatus,
            String operatorType,
            String remark,
            Instant createdAt
    ) {
    }

    public record OrderView(
            String orderNo,
            String status,
            List<OrderItemView> items,
            BigDecimal goodsAmount,
            BigDecimal discountAmount,
            BigDecimal shippingAmount,
            BigDecimal payableAmount,
            String receiverName,
            String receiverPhone,
            String addressLine,
            String customerNote,
            Instant expireAt,
            Instant paidAt,
            Instant shippedAt,
            Instant completedAt,
            Instant closedAt,
            Instant createdAt,
            List<StatusLogView> timeline
    ) {
    }
}
