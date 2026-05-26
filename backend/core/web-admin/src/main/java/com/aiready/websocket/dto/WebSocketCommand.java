package com.aiready.websocket.dto;

import lombok.Data;

/**
 * WebSocket命令
 * 用于接收客户端发送的命令
 */
@Data
public class WebSocketCommand {
    
    /**
     * 命令类型
     * - SUBSCRIBE: 订阅房间
     * - UNSUBSCRIBE: 取消订阅
     * - SEND_MESSAGE: 发送消息
     * - READ_MESSAGE: 标记已读
     * - HEARTBEAT: 心跳
     * - JOIN_ROOM: 加入房间
     * - LEAVE_ROOM: 离开房间
     * - GET_HISTORY: 获取历史消息
     */
    private String command;
    
    /**
     * 命令数据
     */
    private Object data;
    
    /**
     * 房间ID
     */
    private String roomId;
    
    /**
     * 消息ID
     */
    private String messageId;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 目标用户ID（私聊使用）
     */
    private Long targetUserId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息类型
     */
    private Integer messageType;
    
    /**
     * 页码
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer size;
}
