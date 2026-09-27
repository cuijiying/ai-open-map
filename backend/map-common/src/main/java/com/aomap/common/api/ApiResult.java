package com.aomap.common.api;

public record ApiResult<T>(int code, String message, T data) {

    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(0, "ok", data);
    }

    public static <T> ApiResult<T> fail(String message) {
        return new ApiResult<>(1, message, null);
    }
}
