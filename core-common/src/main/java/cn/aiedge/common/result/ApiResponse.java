package cn.aiedge.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 统一响应结果封装
 * 使用Java 17 Record实现不可变数据载体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
    int code,
    String message,
    T data,
    Long timestamp
) {
    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data, System.currentTimeMillis());
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(500, message, null, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, System.currentTimeMillis());
    }

    /**
     * 常用响应
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return fail(404, message);
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return fail(401, message);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return fail(403, message);
    }

    public static <T> ApiResponse<T> badRequest(String message) {
        return fail(400, message);
    }
}