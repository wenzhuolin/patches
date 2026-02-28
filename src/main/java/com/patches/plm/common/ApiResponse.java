package com.patches.plm.common;

public record ApiResponse<T>(String code, String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("0", "OK", data);
    }

    public static <T> ApiResponse<T> failure(String code, String message, T data) {
        return new ApiResponse<>(code, message, data);
    }
}
