package com.aurora.commerce.shared.api;

import java.util.UUID;

public record ApiResponse<T>(String code, String message, T data, String requestId) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("OK", "success", data, UUID.randomUUID().toString());
    }

    public static <T> ApiResponse<T> error(String code, String message, T data) {
        return new ApiResponse<>(code, message, data, UUID.randomUUID().toString());
    }
}
