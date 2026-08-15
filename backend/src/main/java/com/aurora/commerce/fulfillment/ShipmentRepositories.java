package com.aurora.commerce.fulfillment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    Optional<Shipment> findByOrderNo(String orderNo);
}

interface ShipmentTrackRepository extends JpaRepository<ShipmentTrack, Long> {
    List<ShipmentTrack> findByShipmentIdOrderByOccurredAtDesc(Long shipmentId);
}
