package cn.aiedge.base.websocket;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * WebSocket 统一响应体
 * 与前端 src/types/websocket.ts 的 WsResponse 对齐
 */
public class WebSocketResponse<T> {

    private String type;
    private T data;
    private String timestamp;
    private String messageId;
    private Integer code;
    private String error;

    public WebSocketResponse() {}

    public WebSocketResponse(String type, T data) {
        this.type = type;
        this.data = data;
        this.timestamp = LocalDateTime.now().toString();
        this.messageId = UUID.randomUUID().toString().replace("-", "");
        this.code = 200;
    }

    public static <T> WebSocketResponse<T> success(String type, T data) {
        WebSocketResponse<T> resp = new WebSocketResponse<>(type, data);
        resp.code = 200;
        return resp;
    }

    public static <T> WebSocketResponse<T> error(String errorMsg) {
        WebSocketResponse<T> resp = new WebSocketResponse<>();
        resp.type = "ERROR";
        resp.code = 500;
        resp.error = errorMsg;
        resp.timestamp = LocalDateTime.now().toString();
        return resp;
    }

    public static WebSocketResponse<Void> heartbeat() {
        WebSocketResponse<Void> resp = new WebSocketResponse<>();
        resp.type = "HEARTBEAT";
        resp.code = 200;
        resp.timestamp = LocalDateTime.now().toString();
        return resp;
    }

    public static WebSocketResponse<Void> ack(String messageId) {
        WebSocketResponse<Void> resp = new WebSocketResponse<>();
        resp.type = "ACK";
        resp.code = 200;
        resp.messageId = messageId;
        resp.timestamp = LocalDateTime.now().toString();
        return resp;
    }

    // getters / setters

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
