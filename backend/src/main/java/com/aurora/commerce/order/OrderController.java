package com.aurora.commerce.order;

import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
class OrderController {

    private final OrderFacade orderFacade;

    OrderController(OrderFacade orderFacade) {
        this.orderFacade = orderFacade;
    }

    @PostMapping("/orders")
    ApiResponse<OrderFacade.OrderView> create(
            JwtAuthenticationToken authentication,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return ApiResponse.success(orderFacade.create(
                userId(authentication), idempotencyKey, request.toCommand()));
    }

    @GetMapping("/orders")
    ApiResponse<List<OrderFacade.OrderView>> list(JwtAuthenticationToken authentication) {
        return ApiResponse.success(orderFacade.list(userId(authentication)));
    }

    @GetMapping("/orders/{orderNo}")
    ApiResponse<OrderFacade.OrderView> detail(
            JwtAuthenticationToken authentication,
            @PathVariable String orderNo
    ) {
        return ApiResponse.success(orderFacade.detail(userId(authentication), orderNo));
    }

    @PostMapping("/orders/{orderNo}/cancel")
    ApiResponse<OrderFacade.OrderView> cancel(
            JwtAuthenticationToken authentication,
            @PathVariable String orderNo
    ) {
        return ApiResponse.success(orderFacade.cancel(userId(authentication), orderNo));
    }

    @GetMapping("/admin/orders")
    ApiResponse<List<OrderFacade.OrderView>> adminList(
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(orderFacade.adminList(status));
    }

    private Long userId(JwtAuthenticationToken authentication) {
        Number userId = authentication.getToken().getClaim("userId");
        return userId.longValue();
    }

    record CreateOrderRequest(
            @NotBlank @Size(max = 80) String receiverName,
            @NotBlank @Size(max = 30)
            @Pattern(regexp = "^[0-9+() -]{6,30}$") String receiverPhone,
            @NotBlank @Size(max = 300) String addressLine,
            @Size(max = 300) String customerNote,
            Long userCouponId
    ) {
        OrderFacade.CreateOrderCommand toCommand() {
            return new OrderFacade.CreateOrderCommand(
                    receiverName, receiverPhone, addressLine, customerNote, userCouponId);
        }
    }
}
