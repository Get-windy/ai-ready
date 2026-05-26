package cn.aiedge.integration.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * API响应实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "API响应")
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应码
     */
    @Schema(description = "响应码")
    private Integer code;

    /**
     * 响应消息
     */
    @Schema(description = "响应消息")
    private String message;

    /**
     * 响应数据
     */
    @Schema(description = "响应数据")
    private T data;

    /**
     * 请求ID
     */
    @Schema(description = "请求ID")
    private String requestId;

    /**
     * 时间戳
     */
    @Schema(description = "时间戳")
    private Long timestamp;

    /**
     * 签名
     */
    @Schema(description = "签名")
    private String sign;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }

    public static <T> ApiResponse<T> error(Integer code, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }

    // Getters and Setters
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }
}
