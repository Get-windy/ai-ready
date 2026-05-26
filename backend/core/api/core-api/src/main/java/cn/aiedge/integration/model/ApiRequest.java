package cn.aiedge.integration.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.Map;

/**
 * API请求实体
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Schema(description = "API请求")
public class ApiRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 请求ID
     */
    @Schema(description = "请求ID")
    private String requestId;

    /**
     * 请求方法: GET/POST/PUT/DELETE
     */
    @Schema(description = "请求方法")
    private String method;

    /**
     * 请求路径
     */
    @Schema(description = "请求路径")
    private String path;

    /**
     * 请求头
     */
    @Schema(description = "请求头")
    private Map<String, String> headers;

    /**
     * 请求参数
     */
    @Schema(description = "请求参数")
    private Map<String, Object> params;

    /**
     * 请求体
     */
    @Schema(description = "请求体")
    private Object body;

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

    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Map<String, String> getHeaders() { return headers; }
    public void setHeaders(Map<String, String> headers) { this.headers = headers; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    public Object getBody() { return body; }
    public void setBody(Object body) { this.body = body; }

    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }

    public String getSign() { return sign; }
    public void setSign(String sign) { this.sign = sign; }
}
