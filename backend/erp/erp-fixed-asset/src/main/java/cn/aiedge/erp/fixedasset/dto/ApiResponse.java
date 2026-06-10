package cn.aiedge.erp.fixedasset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * API统一响应格式
 */
@Data
@Schema(description = "API统一响应格式")
public class ApiResponse<T> {

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "业务状态码，如 200/400/404/500")
    private String code;

    @Schema(description = "响应消息")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "时间戳")
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
