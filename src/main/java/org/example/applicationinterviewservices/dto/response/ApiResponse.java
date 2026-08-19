package org.example.applicationinterviewservices.dto.response;

import lombok.Builder;

@Builder
public record ApiResponse<T>(boolean success, int statusCode, String message, T data) {

    public static <T> ApiResponse<T> success(int statusCode, String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(statusCode)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(int statusCode, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .statusCode(statusCode)
                .message(message)
                .data(null)
                .build();
    }
}