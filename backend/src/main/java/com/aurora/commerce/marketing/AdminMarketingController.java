package com.aurora.commerce.marketing;

import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/coupons")
class AdminMarketingController {

    private final MarketingFacade marketingFacade;

    AdminMarketingController(MarketingFacade marketingFacade) {
        this.marketingFacade = marketingFacade;
    }

    @GetMapping
    ApiResponse<List<MarketingFacade.CouponView>> list() {
        return ApiResponse.success(marketingFacade.adminList());
    }

    @PostMapping
    ApiResponse<MarketingFacade.CouponView> create(@Valid @RequestBody CreateCouponRequest request) {
        return ApiResponse.success(marketingFacade.create(request.toCommand()));
    }

    record CreateCouponRequest(
            @NotBlank @Size(max = 80) String code,
            @NotBlank @Size(max = 120) String name,
            @Size(max = 300) String description,
            @NotNull @DecimalMin("0.01") BigDecimal discountAmount,
            @NotNull @DecimalMin("0.01") BigDecimal thresholdAmount,
            @Min(1) @Max(10_000_000) int totalLimit,
            @NotNull Instant startsAt,
            @NotNull Instant endsAt
    ) {
        MarketingFacade.CreateCouponCommand toCommand() {
            return new MarketingFacade.CreateCouponCommand(
                    code, name, description, discountAmount, thresholdAmount,
                    totalLimit, startsAt, endsAt);
        }
    }
}
