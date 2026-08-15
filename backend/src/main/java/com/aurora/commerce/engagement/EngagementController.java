package com.aurora.commerce.engagement;

import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
class EngagementController {

    private final EngagementFacade engagementFacade;

    EngagementController(EngagementFacade engagementFacade) {
        this.engagementFacade = engagementFacade;
    }

    @GetMapping("/favorites")
    ApiResponse<List<EngagementFacade.FavoriteView>> favorites(JwtAuthenticationToken authentication) {
        return ApiResponse.success(engagementFacade.favorites(userId(authentication)));
    }

    @PostMapping("/favorites/{productId}")
    ApiResponse<EngagementFacade.FavoriteView> favorite(
            JwtAuthenticationToken authentication, @PathVariable Long productId
    ) {
        return ApiResponse.success(engagementFacade.favorite(userId(authentication), productId));
    }

    @DeleteMapping("/favorites/{productId}")
    ApiResponse<Void> removeFavorite(
            JwtAuthenticationToken authentication, @PathVariable Long productId
    ) {
        engagementFacade.removeFavorite(userId(authentication), productId);
        return ApiResponse.success(null);
    }

    @PostMapping("/reviews")
    ApiResponse<EngagementFacade.ReviewView> createReview(
            JwtAuthenticationToken authentication, @Valid @RequestBody CreateReviewRequest request
    ) {
        return ApiResponse.success(engagementFacade.createReview(
                userId(authentication), request.toCommand()));
    }

    @GetMapping("/products/{productId}/reviews")
    ApiResponse<List<EngagementFacade.ReviewView>> productReviews(@PathVariable Long productId) {
        return ApiResponse.success(engagementFacade.productReviews(productId));
    }

    @GetMapping("/me/reviews")
    ApiResponse<List<EngagementFacade.ReviewView>> myReviews(JwtAuthenticationToken authentication) {
        return ApiResponse.success(engagementFacade.myReviews(userId(authentication)));
    }

    @GetMapping("/admin/reviews")
    ApiResponse<List<EngagementFacade.ReviewView>> adminReviews(
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(engagementFacade.adminReviews(status));
    }

    @PatchMapping("/admin/reviews/{reviewId}/reply")
    ApiResponse<EngagementFacade.ReviewView> reply(
            @PathVariable Long reviewId, @Valid @RequestBody ReplyRequest request
    ) {
        return ApiResponse.success(engagementFacade.reply(reviewId, request.reply()));
    }

    @PatchMapping("/admin/reviews/{reviewId}/hide")
    ApiResponse<EngagementFacade.ReviewView> hide(@PathVariable Long reviewId) {
        return ApiResponse.success(engagementFacade.hide(reviewId));
    }

    private Long userId(JwtAuthenticationToken authentication) {
        Number userId = authentication.getToken().getClaim("userId");
        return userId.longValue();
    }

    record CreateReviewRequest(
            @NotBlank @Size(max = 40) String orderNo,
            @NotNull Long orderItemId,
            @Min(1) @Max(5) int rating,
            @NotBlank @Size(max = 1000) String content,
            @Size(max = 2000) String imageUrls
    ) {
        EngagementFacade.CreateReviewCommand toCommand() {
            return new EngagementFacade.CreateReviewCommand(
                    orderNo, orderItemId, rating, content, imageUrls);
        }
    }

    record ReplyRequest(@NotBlank @Size(max = 1000) String reply) {
    }
}
