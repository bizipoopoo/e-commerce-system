package com.aurora.commerce.fulfillment;

import com.aurora.commerce.order.OrderFacade;
import com.aurora.commerce.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
class FulfillmentService {

    private static final DateTimeFormatter NUMBER_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneOffset.UTC);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackRepository trackRepository;
    private final OrderFacade orderFacade;

    FulfillmentService(
            ShipmentRepository shipmentRepository,
            ShipmentTrackRepository trackRepository,
            OrderFacade orderFacade
    ) {
        this.shipmentRepository = shipmentRepository;
        this.trackRepository = trackRepository;
        this.orderFacade = orderFacade;
    }

    @Transactional
    ShipmentView ship(String orderNo, String carrier, String trackingNo) {
        Shipment existing = shipmentRepository.findByOrderNo(orderNo).orElse(null);
        if (existing != null) {
            if (existing.carrier().equals(carrier) && existing.trackingNo().equals(trackingNo)) {
                return toView(existing);
            }
            throw new BusinessException(
                    "SHIPMENT_ALREADY_EXISTS", "订单已经创建其他物流单", HttpStatus.CONFLICT);
        }
        OrderFacade.OrderReference order = orderFacade.reference(orderNo);
        orderFacade.markShipped(orderNo, "管理员发货：" + carrier + " / " + trackingNo);
        Instant now = Instant.now();
        Shipment shipment = shipmentRepository.save(new Shipment(
                nextNumber(), order.orderId(), orderNo, carrier, trackingNo, now));
        trackRepository.save(new ShipmentTrack(shipment.id(), "商家已发货，包裹等待揽收", now));
        return toView(shipment);
    }

    @Transactional(readOnly = true)
    ShipmentView detail(Long userId, String orderNo) {
        orderFacade.detail(userId, orderNo);
        return toView(shipmentRepository.findByOrderNo(orderNo).orElseThrow(this::shipmentNotFound));
    }

    @Transactional
    OrderFacade.OrderView confirmReceipt(Long userId, String orderNo) {
        orderFacade.detail(userId, orderNo);
        Shipment shipment = shipmentRepository.findByOrderNo(orderNo).orElseThrow(this::shipmentNotFound);
        if (shipment.status() != Shipment.Status.DELIVERED) {
            Instant now = Instant.now();
            shipment.deliver(now);
            trackRepository.save(new ShipmentTrack(shipment.id(), "客户已签收，订单完成", now));
        }
        return orderFacade.confirmReceipt(userId, orderNo);
    }

    private ShipmentView toView(Shipment shipment) {
        List<TrackView> tracks = trackRepository.findByShipmentIdOrderByOccurredAtDesc(shipment.id()).stream()
                .map(track -> new TrackView(track.description(), track.occurredAt())).toList();
        return new ShipmentView(
                shipment.shipmentNo(), shipment.orderNo(), shipment.status().name(), shipment.carrier(),
                shipment.trackingNo(), shipment.shippedAt(), shipment.deliveredAt(), tracks);
    }

    private String nextNumber() {
        return "SHP" + NUMBER_TIME.format(Instant.now())
                + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private BusinessException shipmentNotFound() {
        return new BusinessException("SHIPMENT_NOT_FOUND", "物流单不存在", HttpStatus.NOT_FOUND);
    }

    record TrackView(String description, Instant occurredAt) {
    }

    record ShipmentView(
            String shipmentNo,
            String orderNo,
            String status,
            String carrier,
            String trackingNo,
            Instant shippedAt,
            Instant deliveredAt,
            List<TrackView> tracks
    ) {
    }
}
