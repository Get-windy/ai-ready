package com.aiready.websocket.dto;

import lombok.Data;

/**
 * 发送消息请求
 */
@Data
public class SendMessageRequest {
    
    /**
     * 消息类型（1：系统通知 2：业务通知 3：私聊消息 4：群聊消息 5：广播消息）
     */
    private Integer messageType;
    
    /**
     * 接收者ID（私聊使用）
     */
    private Long receiverId;
    
    /**
     * 房间ID（群聊使用）
     */
    private String roomId;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息内容类型（1：文本 2：图片 3：文件 4：富文本）
     */
    private Integer contentType;
    
    /**
     * 附加数据（JSON格式）
     */
    private String extraData;
    
    /**
     * 业务类型
     */
    private String bizType;
    
    /**
     * 业务ID
     */
    private String bizId;
}
