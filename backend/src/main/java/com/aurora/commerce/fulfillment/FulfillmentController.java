package com.aurora.commerce.fulfillment;

import com.aurora.commerce.order.OrderFacade;
import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    FulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @PostMapping("/admin/orders/{orderNo}/ship")
    ApiResponse<FulfillmentService.ShipmentView> ship(
            @PathVariable String orderNo,
            @Valid @RequestBody ShipRequest request
    ) {
        return ApiResponse.success(fulfillmentService.ship(
                orderNo, request.carrier(), request.trackingNo()));
    }

    @GetMapping("/shipments/{orderNo}")
    ApiResponse<FulfillmentService.ShipmentView> shipment(
            JwtAuthenticationToken authentication,
            @PathVariable String orderNo
    ) {
        return ApiResponse.success(fulfillmentService.detail(userId(authentication), orderNo));
    }

    @PostMapping("/orders/{orderNo}/confirm-receipt")
    ApiResponse<OrderFacade.OrderView> confirmReceipt(
            JwtAuthenticationToken authentication,
            @PathVariable String orderNo
    ) {
        return ApiResponse.success(fulfillmentService.confirmReceipt(userId(authentication), orderNo));
    }

    private Long userId(JwtAuthenticationToken authentication) {
        Number userId = authentication.getToken().getClaim("userId");
        return userId.longValue();
    }

    record ShipRequest(
            @NotBlank @Size(max = 80) String carrier,
            @NotBlank @Size(max = 100) String trackingNo
    ) {
    }
}
