package cn.aiedge.erp.budget.dto;

import lombok.Data;

/**
 * API统一响应格式
 */
@Data
public class ApiResponse<T> {

    private boolean success;
    private String code;
    private String message;
    private T data;
    private Long timestamp;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code = "200";
        response.message = "Success";
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = true;
        response.code = "200";
        response.message = message;
        response.data = data;
        return response;
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.success = false;
        response.code = code;
        response.message = message;
        response.data = null;
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        return error("500", message);
    }

    public static <T> ApiResponse<T> validationError(String message) {
        return error("400", message);
    }

    public static <T> ApiResponse<T> notFound(String message) {
        return error("404", message);
    }

    public static <T> ApiResponse<T> unauthorized(String message) {
        return error("401", message);
    }

    public static <T> ApiResponse<T> forbidden(String message) {
        return error("403", message);
    }
}
