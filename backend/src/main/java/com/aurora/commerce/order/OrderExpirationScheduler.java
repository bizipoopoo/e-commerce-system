package com.aurora.commerce.order;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
class OrderExpirationScheduler {

    private final OrderFacade orderFacade;

    OrderExpirationScheduler(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }

    @Scheduled(fixedDelayString = "${commerce.order.expiration-scan-delay:60000}")
    void closeExpiredOrders() {
        orderFacade.expirePendingOrders(Instant.now());
    }
}
