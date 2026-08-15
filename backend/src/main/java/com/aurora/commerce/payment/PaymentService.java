package com.aurora.commerce.payment;

import com.aurora.commerce.order.OrderFacade;
import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
class PaymentService {

    private static final DateTimeFormatter NUMBER_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final PaymentOrderRepository paymentRepository;
    private final PaymentCallbackRepository callbackRepository;
    private final OrderFacade orderFacade;

    PaymentService(
            PaymentOrderRepository paymentRepository,
            PaymentCallbackRepository callbackRepository,
            OrderFacade orderFacade
    ) {
        this.paymentRepository = paymentRepository;
        this.callbackRepository = callbackRepository;
        this.orderFacade = orderFacade;
    }

    @Transactional
    PaymentView create(Long userId, String orderNo, String channel) {
        if (!"MOCK".equalsIgnoreCase(channel)) {
            throw new BusinessException("PAYMENT_CHANNEL_NOT_SUPPORTED", "当前仅支持模拟支付", HttpStatus.BAD_REQUEST);
        }
        OrderFacade.PaymentTarget target = orderFacade.paymentTarget(userId, orderNo);
        PaymentOrder existing = paymentRepository.findByOrderId(target.orderId()).orElse(null);
        if (existing != null) {
            return toView(existing, orderNo);
        }
        if (!"PENDING_PAYMENT".equals(target.status())) {
            throw new BusinessException(
                    "ORDER_STATE_CONFLICT", "当前订单状态不允许支付", HttpStatus.CONFLICT);
        }
        PaymentOrder payment = paymentRepository.save(new PaymentOrder(
                nextNumber(), target.orderId(), target.orderNo(), "MOCK", target.amount()));
        return toView(payment, orderNo);
    }

    @Transactional
    PaymentView completeMock(Long userId, String paymentNo) {
        PaymentOrder payment = paymentRepository.findByPaymentNoForUpdate(paymentNo)
                .orElseThrow(this::paymentNotFound);
        OrderFacade.OrderView order = orderFacade.detail(userId, payment.orderNo());
        if (payment.status() == PaymentOrder.Status.SUCCESS) {
            return toView(payment, order.orderNo());
        }
        String eventId = "MOCK:" + payment.paymentNo();
        Instant now = Instant.now();
        callbackRepository.save(new PaymentCallback(
                payment.id(), eventId, "{\"result\":\"SUCCESS\"}", now));
        orderFacade.markPaid(order.orderNo(), payment.amount());
        payment.succeed("TRADE-" + payment.paymentNo(), now);
        return toView(payment, order.orderNo());
    }

    private PaymentView toView(PaymentOrder payment, String orderNo) {
        return new PaymentView(
                payment.paymentNo(), orderNo, payment.status().name(), payment.channel(), payment.amount(),
                payment.providerTradeNo(), payment.paidAt(), payment.createdAt());
    }

    private String nextNumber() {
        return "PAY" + NUMBER_TIME.format(Instant.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private BusinessException paymentNotFound() {
        return new BusinessException("PAYMENT_NOT_FOUND", "支付单不存在", HttpStatus.NOT_FOUND);
    }

    record PaymentView(
            String paymentNo,
            String orderNo,
            String status,
            String channel,
            java.math.BigDecimal amount,
            String providerTradeNo,
            Instant paidAt,
            Instant createdAt
    ) {
    }
}
