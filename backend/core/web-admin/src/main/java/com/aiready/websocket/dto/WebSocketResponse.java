package com.aiready.websocket.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * WebSocket响应消息
 * 用于WebSocket通信的消息格式
 */
@Data
public class WebSocketResponse<T> {
    
    /**
     * 消息类型
     * - CONNECT: 连接成功
     * - DISCONNECT: 断开连接
     * - HEARTBEAT: 心跳
     * - MESSAGE: 普通消息
     * - SYSTEM: 系统通知
     * - ERROR: 错误消息
     * - ACK: 确认收到
     */
    private String type;
    
    /**
     * 消息内容
     */
    private T data;
    
    /**
     * 消息时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 错误码（错误消息使用）
     */
    private Integer code;
    
    /**
     * 错误信息（错误消息使用）
     */
    private String error;
    
    public WebSocketResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public static <T> WebSocketResponse<T> success(String type, T data) {
        WebSocketResponse<T> response = new WebSocketResponse<>();
        response.setType(type);
        response.setData(data);
        return response;
    }
    
    public static <T> WebSocketResponse<T> error(String errorMessage) {
        WebSocketResponse<T> response = new WebSocketResponse<>();
        response.setType("ERROR");
        response.setError(errorMessage);
        return response;
    }
    
    public static <T> WebSocketResponse<T> error(Integer code, String errorMessage) {
        WebSocketResponse<T> response = new WebSocketResponse<>();
        response.setType("ERROR");
        response.setCode(code);
        response.setError(errorMessage);
        return response;
    }
    
    public static <T> WebSocketResponse<T> heartbeat() {
        WebSocketResponse<T> response = new WebSocketResponse<>();
        response.setType("HEARTBEAT");
        return response;
    }
    
    public static <T> WebSocketResponse<T> ack(String messageId) {
        WebSocketResponse<T> response = new WebSocketResponse<>();
        response.setType("ACK");
        response.setMessageId(messageId);
        return response;
    }
}
