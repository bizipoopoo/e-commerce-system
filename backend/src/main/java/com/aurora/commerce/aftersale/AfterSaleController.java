package com.aurora.commerce.aftersale;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
class AfterSaleController {

    private final AfterSaleFacade afterSaleFacade;

    AfterSaleController(AfterSaleFacade afterSaleFacade) {
        this.afterSaleFacade = afterSaleFacade;
    }

    @PostMapping("/after-sales")
    ApiResponse<AfterSaleFacade.AfterSaleView> apply(
            JwtAuthenticationToken authentication, @Valid @RequestBody ApplyRequest request
    ) {
        return ApiResponse.success(afterSaleFacade.apply(userId(authentication), request.toCommand()));
    }

    @GetMapping("/after-sales")
    ApiResponse<List<AfterSaleFacade.AfterSaleView>> mine(JwtAuthenticationToken authentication) {
        return ApiResponse.success(afterSaleFacade.mine(userId(authentication)));
    }

    @GetMapping("/after-sales/{afterSaleNo}")
    ApiResponse<AfterSaleFacade.AfterSaleView> detail(
            JwtAuthenticationToken authentication, @PathVariable String afterSaleNo
    ) {
        return ApiResponse.success(afterSaleFacade.detail(userId(authentication), afterSaleNo));
    }

    @PostMapping("/after-sales/{afterSaleNo}/return")
    ApiResponse<AfterSaleFacade.AfterSaleView> submitReturn(
            JwtAuthenticationToken authentication, @PathVariable String afterSaleNo,
            @Valid @RequestBody ReturnRequest request
    ) {
        return ApiResponse.success(afterSaleFacade.submitReturn(
                userId(authentication), afterSaleNo, request.toCommand()));
    }

    @GetMapping("/admin/after-sales")
    ApiResponse<List<AfterSaleFacade.AfterSaleView>> adminList(
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(afterSaleFacade.adminList(status));
    }

    @PostMapping("/admin/after-sales/{afterSaleNo}/approve")
    ApiResponse<AfterSaleFacade.AfterSaleView> approve(
            @PathVariable String afterSaleNo, @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.success(afterSaleFacade.approve(afterSaleNo, request.note()));
    }

    @PostMapping("/admin/after-sales/{afterSaleNo}/reject")
    ApiResponse<AfterSaleFacade.AfterSaleView> reject(
            @PathVariable String afterSaleNo, @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.success(afterSaleFacade.reject(afterSaleNo, request.note()));
    }

    @PostMapping("/admin/after-sales/{afterSaleNo}/refund")
    ApiResponse<AfterSaleFacade.AfterSaleView> refund(
            @PathVariable String afterSaleNo, @Valid @RequestBody ReviewRequest request
    ) {
        return ApiResponse.success(afterSaleFacade.refund(afterSaleNo, request.note()));
    }

    private Long userId(JwtAuthenticationToken authentication) {
        Number userId = authentication.getToken().getClaim("userId");
        return userId.longValue();
    }

    record ApplyRequest(
            @NotBlank @Size(max = 40) String orderNo,
            @NotBlank @Pattern(regexp = "REFUND_ONLY|RETURN_REFUND") String type,
            @NotBlank @Size(max = 40) String reasonCode,
            @NotBlank @Size(max = 1000) String description
    ) {
        AfterSaleFacade.ApplyCommand toCommand() {
            return new AfterSaleFacade.ApplyCommand(orderNo, type, reasonCode, description);
        }
    }

    record ReturnRequest(
            @NotBlank @Size(max = 80) String carrier,
            @NotBlank @Size(max = 100) String trackingNo
    ) {
        AfterSaleFacade.ReturnCommand toCommand() {
            return new AfterSaleFacade.ReturnCommand(carrier, trackingNo);
        }
    }

    record ReviewRequest(@NotBlank @Size(max = 500) String note) {
    }
}
