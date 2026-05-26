package cn.aiedge.common.result;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * API统一响应类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "API统一响应")
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 状态码
     */
    @Schema(description = "状态码")
    private int code;

    /**
     * 消息
     */
    @Schema(description = "消息")
    private String message;

    /**
     * 数据
     */
    @Schema(description = "数据")
    private T data;

    /**
     * 是否成功
     */
    @Schema(description = "是否成功")
    private boolean success;

    public ApiResponse() {
    }

    public ApiResponse(int code, String message, T data, boolean success) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = success;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(200, "成功", null, true);
    }

    /**
     * 成功响应（有数据）
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "成功", data, true);
    }

    /**
     * 成功响应（有数据和消息）
     */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(200, message, data, true);
    }

    /**
     * 成功响应（无数据） - 别名
     */
    public static <T> ApiResponse<T> success() {
        return ok();
    }

    /**
     * 成功响应（有数据） - 别名
     */
    public static <T> ApiResponse<T> success(T data) {
        return ok(data);
    }

    /**
     * 成功响应（有数据和消息） - 别名
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ok(data, message);
    }

    /**
 * 成功响应（带消息，无数据） - 控制器常用签名
     */
    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(200, message, null, true);
    }

    /**
     * 成功响应（带消息和 ID 数据） - 控制器常用签名
     */
    public static ApiResponse<Long> okWithId(String message, Long id) {
        return new ApiResponse<>(200, message, id, true);
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null, false);
    }

    /**
     * 失败响应（自定义状态码）
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, false);
    }

    /**
     * 失败响应（带数据）
     */
    public static <T> ApiResponse<T> error(int code, String message, T data) {
        return new ApiResponse<>(code, message, data, false);
    }

    // ==================== Getters and Setters ====================

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
