package com.aurora.commerce.operations;

import com.aurora.commerce.aftersale.AfterSaleFacade;
import com.aurora.commerce.inventory.InventoryFacade;
import com.aurora.commerce.order.OrderFacade;
import com.aurora.commerce.shared.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
class OperationsController {

    private static final Set<String> PAID_STATUSES =
            Set.of("PAID", "SHIPPED", "COMPLETED", "REFUNDING");

    private final OrderFacade orderFacade;
    private final InventoryFacade inventoryFacade;
    private final AfterSaleFacade afterSaleFacade;

    OperationsController(
            OrderFacade orderFacade,
            InventoryFacade inventoryFacade,
            AfterSaleFacade afterSaleFacade
    ) {
        this.orderFacade = orderFacade;
        this.inventoryFacade = inventoryFacade;
        this.afterSaleFacade = afterSaleFacade;
    }

    @GetMapping
    ApiResponse<DashboardView> dashboard() {
        List<OrderFacade.OrderView> orders = orderFacade.adminList(null);
        BigDecimal gmv = orders.stream().filter(order -> PAID_STATUSES.contains(order.status()))
                .map(OrderFacade.OrderView::payableAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidOrders = orders.stream().filter(order -> PAID_STATUSES.contains(order.status())).count();
        long pendingShipment = orders.stream().filter(order -> "PAID".equals(order.status())).count();
        long completed = orders.stream().filter(order -> "COMPLETED".equals(order.status())).count();
        long warningStock = inventoryFacade.allStocks(true).size();
        return ApiResponse.success(new DashboardView(
                gmv, paidOrders, pendingShipment, completed, afterSaleFacade.pendingCount(), warningStock,
                orders.stream().limit(6).toList()));
    }

    record DashboardView(
            BigDecimal grossMerchandiseValue,
            long paidOrderCount,
            long pendingShipmentCount,
            long completedOrderCount,
            long pendingAfterSaleCount,
            long warningStockCount,
            List<OrderFacade.OrderView> latestOrders
    ) {
    }
}
