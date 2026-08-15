package com.aurora.commerce.order;

enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    SHIPPED,
    COMPLETED,
    REFUNDING,
    REFUNDED,
    CLOSED
}
