package cn.aiedge.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 统一响应结果封装
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final int code;
    private final String message;
    private final T data;
    private final Long timestamp;

    public ApiResponse(int code, String message, T data, Long timestamp) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
    }

    @JsonProperty("code")
    public int getCode() {
        return code;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @JsonProperty("data")
    public T getData() {
        return data;
    }

    @JsonProperty("timestamp")
    public Long getTimestamp() {
        return timestamp;
    }

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

    /**
     * success别名方法（兼容旧代码）
     */
    public static <T> ApiResponse<T> success() {
        return ok(null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return ok(data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return ok(message, data);
    }

    /**
     * success(Long) - 返回Long类型的响应
     * 注：Java泛型不支持基本类型约束，此方法直接指定泛型为Long
     */
    public static ApiResponse<Long> success(Long id) {
        return new ApiResponse<>(200, "success", id, System.currentTimeMillis());
    }
}