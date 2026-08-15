package com.aurora.commerce.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "payment_callbacks")
class PaymentCallback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;

    @Column(name = "provider_event_id", nullable = false, length = 100)
    private String providerEventId;

    @Column(length = 2000)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentCallback() {
    }

    PaymentCallback(Long paymentId, String providerEventId, String payload, Instant createdAt) {
        this.paymentId = paymentId;
        this.providerEventId = providerEventId;
        this.payload = payload;
        this.createdAt = createdAt;
    }
}
