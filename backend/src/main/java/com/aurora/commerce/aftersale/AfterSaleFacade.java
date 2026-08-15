package com.aurora.commerce.aftersale;

import com.aurora.commerce.inventory.InventoryFacade;
import com.aurora.commerce.notification.NotificationFacade;
import com.aurora.commerce.order.OrderFacade;
import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AfterSaleFacade {

    private static final DateTimeFormatter NUMBER_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final AfterSaleRepository afterSaleRepository;
    private final AfterSaleLogRepository logRepository;
    private final RefundRecordRepository refundRepository;
    private final OrderFacade orderFacade;
    private final InventoryFacade inventoryFacade;
    private final NotificationFacade notificationFacade;

    AfterSaleFacade(
            AfterSaleRepository afterSaleRepository,
            AfterSaleLogRepository logRepository,
            RefundRecordRepository refundRepository,
            OrderFacade orderFacade,
            InventoryFacade inventoryFacade,
            NotificationFacade notificationFacade
    ) {
        this.afterSaleRepository = afterSaleRepository;
        this.logRepository = logRepository;
        this.refundRepository = refundRepository;
        this.orderFacade = orderFacade;
        this.inventoryFacade = inventoryFacade;
        this.notificationFacade = notificationFacade;
    }

    @Transactional
    public AfterSaleView apply(Long userId, ApplyCommand command) {
        AfterSale existing = afterSaleRepository.findByOrderNoAndUserId(command.orderNo(), userId).orElse(null);
        if (existing != null) return toView(existing);
        AfterSale.Type type = parseType(command.type());
        OrderFacade.AfterSaleOrder order = orderFacade.beginAfterSale(userId, command.orderNo(), type.name());
        AfterSale afterSale = afterSaleRepository.save(new AfterSale(
                nextNumber("AS"), order.orderId(), order.orderNo(), userId, type, order.sourceStatus(),
                command.reasonCode().toUpperCase(), command.description(), order.refundAmount()));
        log(afterSale, null, "CUSTOMER", "用户提交售后申请");
        notificationFacade.notifyUser(
                userId, "AFTER_SALE", "售后申请已提交", "售后单已创建，运营人员将尽快审核。",
                "AFTER_SALE", afterSale.afterSaleNo());
        return toView(afterSale);
    }

    @Transactional(readOnly = true)
    public List<AfterSaleView> mine(Long userId) {
        return afterSaleRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public AfterSaleView detail(Long userId, String afterSaleNo) {
        return toView(afterSaleRepository.findByAfterSaleNoAndUserId(afterSaleNo, userId)
                .orElseThrow(this::notFound));
    }

    @Transactional
    public AfterSaleView submitReturn(Long userId, String afterSaleNo, ReturnCommand command) {
        AfterSale afterSale = locked(afterSaleNo);
        if (!afterSale.userId().equals(userId)) throw notFound();
        String from = afterSale.status().name();
        afterSale.submitReturn(command.carrier(), command.trackingNo(), Instant.now());
        log(afterSale, from, "CUSTOMER", "用户已寄回商品：" + command.carrier());
        notificationFacade.notifyUser(
                userId, "AFTER_SALE", "寄回信息已提交", "退货包裹信息已登记，等待运营确认退款。",
                "AFTER_SALE", afterSaleNo);
        return toView(afterSale);
    }

    @Transactional(readOnly = true)
    public List<AfterSaleView> adminList(String status) {
        List<AfterSale> cases;
        if (status == null || status.isBlank()) {
            cases = afterSaleRepository.findAllByOrderByCreatedAtDesc();
        } else {
            try {
                cases = afterSaleRepository.findByStatusOrderByCreatedAtDesc(
                        AfterSale.Status.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException exception) {
                throw new BusinessException(
                        "INVALID_AFTER_SALE_STATUS", "售后状态无效", HttpStatus.BAD_REQUEST);
            }
        }
        return cases.stream().map(this::toView).toList();
    }

    @Transactional
    public AfterSaleView approve(String afterSaleNo, String note) {
        AfterSale afterSale = locked(afterSaleNo);
        String from = afterSale.status().name();
        AfterSale.Status target = afterSale.approve(note, Instant.now());
        log(afterSale, from, "ADMIN", note);
        notificationFacade.notifyUser(
                afterSale.userId(), "AFTER_SALE", "售后审核通过",
                target == AfterSale.Status.WAITING_RETURN ? "请在售后详情中填写寄回物流信息。" : "退款申请已通过，等待退款处理。",
                "AFTER_SALE", afterSaleNo);
        return toView(afterSale);
    }

    @Transactional
    public AfterSaleView reject(String afterSaleNo, String note) {
        AfterSale afterSale = locked(afterSaleNo);
        String from = afterSale.status().name();
        afterSale.reject(note, Instant.now());
        orderFacade.rejectAfterSale(afterSale.orderNo(), afterSale.sourceOrderStatus(), "售后审核驳回");
        log(afterSale, from, "ADMIN", note);
        notificationFacade.notifyUser(
                afterSale.userId(), "AFTER_SALE", "售后申请未通过", note,
                "AFTER_SALE", afterSaleNo);
        return toView(afterSale);
    }

    @Transactional
    public AfterSaleView refund(String afterSaleNo, String note) {
        AfterSale afterSale = locked(afterSaleNo);
        if (afterSale.status() == AfterSale.Status.REFUNDED) return toView(afterSale);
        if (afterSale.status() != AfterSale.Status.APPROVED
                && afterSale.status() != AfterSale.Status.RETURNED) {
            throw stateConflict();
        }
        OrderFacade.AfterSaleOrder order = orderSnapshot(afterSale);
        Map<Long, Integer> quantities = order.items().stream().collect(Collectors.toMap(
                OrderFacade.AfterSaleOrderItem::skuId,
                OrderFacade.AfterSaleOrderItem::quantity,
                Integer::sum));
        inventoryFacade.restock("AFTER_SALE:" + afterSaleNo, quantities);
        orderFacade.completeRefund(afterSale.orderNo(), "模拟退款完成");
        Instant now = Instant.now();
        refundRepository.save(new RefundRecord(
                nextNumber("RF"), afterSale.id(), afterSale.orderNo(), afterSale.refundAmount(), now));
        String from = afterSale.status().name();
        afterSale.refund(now);
        log(afterSale, from, "ADMIN", note);
        notificationFacade.notifyUser(
                afterSale.userId(), "AFTER_SALE", "退款已完成",
                "¥" + afterSale.refundAmount().toPlainString() + " 已通过模拟渠道退回。",
                "AFTER_SALE", afterSaleNo);
        return toView(afterSale);
    }

    @Transactional(readOnly = true)
    public long pendingCount() {
        return afterSaleRepository.countByStatus(AfterSale.Status.PENDING_REVIEW);
    }

    private OrderFacade.AfterSaleOrder orderSnapshot(AfterSale afterSale) {
        return new OrderFacade.AfterSaleOrder(
                afterSale.orderId(), afterSale.orderNo(), afterSale.userId(),
                afterSale.sourceOrderStatus(), afterSale.refundAmount(),
                orderFacade.afterSaleItems(afterSale.orderNo()));
    }

    private void log(AfterSale afterSale, String from, String operator, String remark) {
        logRepository.save(new AfterSaleLog(
                afterSale.id(), from, afterSale.status().name(), operator, remark));
    }

    private AfterSaleView toView(AfterSale afterSale) {
        List<LogView> timeline = logRepository.findByAfterSaleIdOrderByCreatedAtAsc(afterSale.id()).stream()
                .map(log -> new LogView(
                        log.fromStatus(), log.toStatus(), log.operatorType(), log.remark(), log.createdAt()))
                .toList();
        RefundRecord refund = refundRepository.findByAfterSaleId(afterSale.id()).orElse(null);
        return new AfterSaleView(
                afterSale.afterSaleNo(), afterSale.orderNo(), afterSale.userId(), afterSale.type().name(),
                afterSale.sourceOrderStatus(), afterSale.status().name(), afterSale.reasonCode(),
                afterSale.descriptionText(), afterSale.refundAmount(), afterSale.returnCarrier(),
                afterSale.returnTrackingNo(), afterSale.adminNote(), afterSale.reviewedAt(),
                afterSale.returnedAt(), afterSale.refundedAt(), afterSale.createdAt(),
                refund == null ? null : refund.refundNo(), timeline);
    }

    private AfterSale locked(String afterSaleNo) {
        return afterSaleRepository.findByAfterSaleNoForUpdate(afterSaleNo).orElseThrow(this::notFound);
    }

    private AfterSale.Type parseType(String type) {
        try {
            return AfterSale.Type.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException("INVALID_AFTER_SALE_TYPE", "售后类型无效", HttpStatus.BAD_REQUEST);
        }
    }

    private String nextNumber(String prefix) {
        return prefix + NUMBER_TIME.format(Instant.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private BusinessException notFound() {
        return new BusinessException("AFTER_SALE_NOT_FOUND", "售后单不存在", HttpStatus.NOT_FOUND);
    }

    private BusinessException stateConflict() {
        return new BusinessException(
                "AFTER_SALE_STATE_CONFLICT", "当前售后状态不允许此操作", HttpStatus.CONFLICT);
    }

    public record ApplyCommand(String orderNo, String type, String reasonCode, String description) {
    }
    public record ReturnCommand(String carrier, String trackingNo) {
    }
    public record LogView(String fromStatus, String toStatus, String operatorType, String remark, Instant createdAt) {
    }
    public record AfterSaleView(
            String afterSaleNo, String orderNo, Long userId, String type, String sourceOrderStatus,
            String status, String reasonCode, String description, java.math.BigDecimal refundAmount,
            String returnCarrier, String returnTrackingNo, String adminNote, Instant reviewedAt,
            Instant returnedAt, Instant refundedAt, Instant createdAt, String refundNo,
            List<LogView> timeline
    ) {
    }
}
