package com.aurora.commerce.payment;

import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
class PaymentController {

    private final PaymentService paymentService;

    PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    ApiResponse<PaymentService.PaymentView> create(
            JwtAuthenticationToken authentication,
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        return ApiResponse.success(paymentService.create(
                userId(authentication), request.orderNo(), request.channel()));
    }

    @PostMapping("/mock/{paymentNo}/complete")
    ApiResponse<PaymentService.PaymentView> completeMock(
            JwtAuthenticationToken authentication,
            @PathVariable String paymentNo
    ) {
        return ApiResponse.success(paymentService.completeMock(userId(authentication), paymentNo));
    }

    private Long userId(JwtAuthenticationToken authentication) {
        Number userId = authentication.getToken().getClaim("userId");
        return userId.longValue();
    }

    record CreatePaymentRequest(
            @NotBlank @Size(max = 40) String orderNo,
            @NotBlank @Size(max = 32) String channel
    ) {
    }
}
