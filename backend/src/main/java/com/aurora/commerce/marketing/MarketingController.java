package com.aurora.commerce.marketing;

import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
class MarketingController {

    private final MarketingFacade marketingFacade;

    MarketingController(MarketingFacade marketingFacade) {
        this.marketingFacade = marketingFacade;
    }

    @GetMapping("/coupons")
    ApiResponse<List<MarketingFacade.CouponView>> available(JwtAuthenticationToken authentication) {
        return ApiResponse.success(marketingFacade.available(userId(authentication)));
    }

    @PostMapping("/coupons/{couponId}/claim")
    ApiResponse<MarketingFacade.CouponView> claim(
            JwtAuthenticationToken authentication,
            @PathVariable Long couponId
    ) {
        return ApiResponse.success(marketingFacade.claim(userId(authentication), couponId));
    }

    @GetMapping("/me/coupons")
    ApiResponse<List<MarketingFacade.CouponView>> mine(JwtAuthenticationToken authentication) {
        return ApiResponse.success(marketingFacade.mine(userId(authentication)));
    }

    @PostMapping("/coupons/preview")
    ApiResponse<MarketingFacade.DiscountView> preview(
            JwtAuthenticationToken authentication,
            @Valid @RequestBody CouponPreviewRequest request
    ) {
        return ApiResponse.success(marketingFacade.preview(
                userId(authentication), request.userCouponId(), request.goodsAmount()));
    }

    private Long userId(JwtAuthenticationToken authentication) {
        Number userId = authentication.getToken().getClaim("userId");
        return userId.longValue();
    }

    record CouponPreviewRequest(
            Long userCouponId,
            @NotNull @DecimalMin("0.00") BigDecimal goodsAmount
    ) {
    }
}
