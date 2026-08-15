package com.aurora.commerce.fulfillment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "shipment_tracks")
class ShipmentTrack {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected ShipmentTrack() {
    }

    ShipmentTrack(Long shipmentId, String description, Instant occurredAt) {
        this.shipmentId = shipmentId;
        this.description = description;
        this.occurredAt = occurredAt;
    }

    String description() { return description; }
    Instant occurredAt() { return occurredAt; }
}
