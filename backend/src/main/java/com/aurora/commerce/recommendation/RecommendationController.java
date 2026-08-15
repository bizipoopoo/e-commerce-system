package com.aurora.commerce.recommendation;

import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
class RecommendationController {

    private final RecommendationService recommendationService;

    RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/recommendations")
    ApiResponse<List<RecommendationService.RecommendationView>> recommendations(Authentication authentication) {
        return ApiResponse.success(recommendationService.recommendations(userId(authentication)));
    }

    @PostMapping("/behaviors")
    ApiResponse<Void> record(
            JwtAuthenticationToken authentication,
            @Valid @RequestBody BehaviorRequest request
    ) {
        recommendationService.record(
                userId(authentication), request.eventType(), request.targetType(),
                request.targetId(), request.contextJson());
        return ApiResponse.success(null);
    }

    private Long userId(Authentication authentication) {
        if (!(authentication instanceof JwtAuthenticationToken jwt)) return null;
        Number userId = jwt.getToken().getClaim("userId");
        return userId.longValue();
    }

    record BehaviorRequest(
            @NotBlank @Size(max = 40) String eventType,
            @NotBlank @Size(max = 40) String targetType,
            @NotNull Long targetId,
            @Size(max = 2000) String contextJson
    ) {
    }
}
