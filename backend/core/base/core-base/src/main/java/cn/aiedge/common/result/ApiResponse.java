package cn.aiedge.common.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * 统一响应结果封装（全局唯一实现，原 core-api 同名类已合并删除）
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int code;
    private final String message;
    private final T data;
    private final Long timestamp;
    private final boolean success;

    public ApiResponse(int code, String message, T data, Long timestamp) {
        this(code, message, data, timestamp, code >= 200 && code < 300);
    }

    public ApiResponse(int code, String message, T data, Long timestamp, boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = timestamp;
        this.success = success;
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

    @JsonProperty("success")
    public boolean isSuccess() {
        return success;
    }

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(200, "success", null, System.currentTimeMillis(), true);
    }

    /**
     * 成功响应
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "success", data, System.currentTimeMillis(), true);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(200, message, data, System.currentTimeMillis(), true);
    }

    /**
     * 成功响应（带消息和 ID 数据） - 控制器常用签名
     */
    public static ApiResponse<Long> okWithId(String message, Long id) {
        return new ApiResponse<>(200, message, id, System.currentTimeMillis(), true);
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(500, message, null, System.currentTimeMillis(), false);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, System.currentTimeMillis(), false);
    }

    /**
     * 失败响应（error 别名，兼容原 core-api 同名类签名）
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null, System.currentTimeMillis(), false);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, System.currentTimeMillis(), false);
    }

    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data, System.currentTimeMillis(), false);
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
        return new ApiResponse<>(200, "success", id, System.currentTimeMillis(), true);
    }
}
