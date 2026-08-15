package com.aurora.commerce.notification;

import com.aurora.commerce.shared.api.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1")
class NotificationController {

    private final NotificationFacade notificationFacade;

    NotificationController(NotificationFacade notificationFacade) {
        this.notificationFacade = notificationFacade;
    }

    @GetMapping("/notifications")
    ApiResponse<NotificationFacade.NotificationCenter> center(JwtAuthenticationToken authentication) {
        return ApiResponse.success(notificationFacade.center(userId(authentication)));
    }

    @PatchMapping("/notifications/{id}/read")
    ApiResponse<NotificationFacade.NotificationView> markRead(
            JwtAuthenticationToken authentication,
            @PathVariable Long id
    ) {
        return ApiResponse.success(notificationFacade.markRead(userId(authentication), id));
    }

    @GetMapping(value = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(JwtAuthenticationToken authentication) {
        return notificationFacade.subscribe(userId(authentication));
    }

    @PostMapping("/admin/notifications")
    ApiResponse<NotificationFacade.NotificationView> send(
            @Valid @RequestBody SendNotificationRequest request
    ) {
        return ApiResponse.success(notificationFacade.notifyUser(
                request.userId(), request.type(), request.title(), request.content(),
                request.referenceType(), request.referenceNo()));
    }

    private Long userId(JwtAuthenticationToken authentication) {
        Number userId = authentication.getToken().getClaim("userId");
        return userId.longValue();
    }

    record SendNotificationRequest(
            @NotNull Long userId,
            @NotBlank @Size(max = 40) String type,
            @NotBlank @Size(max = 180) String title,
            @NotBlank @Size(max = 1000) String content,
            @Size(max = 40) String referenceType,
            @Size(max = 100) String referenceNo
    ) {
    }
}
