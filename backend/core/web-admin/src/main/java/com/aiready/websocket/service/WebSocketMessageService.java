package com.aiready.websocket.service;

import com.aiready.websocket.dto.MessageQueryRequest;
import com.aiready.websocket.dto.SendMessageRequest;
import com.aiready.websocket.dto.WebSocketMessageDTO;
import com.aiready.websocket.entity.WebSocketMessage;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * WebSocket消息服务接口
 */
public interface WebSocketMessageService extends IService<WebSocketMessage> {
    
    /**
     * 发送消息
     */
    WebSocketMessageDTO sendMessage(Long senderId, SendMessageRequest request);
    
    /**
     * 发送系统通知
     */
    WebSocketMessageDTO sendSystemNotification(Long userId, String title, String content, String bizType, String bizId);
    
    /**
     * 广播消息（发送给所有在线用户）
     */
    WebSocketMessageDTO broadcastMessage(Long senderId, String content, String bizType);
    
    /**
     * 查询房间消息列表
     */
    IPage<WebSocketMessageDTO> getRoomMessages(String roomId, Integer page, Integer size);
    
    /**
     * 查询私聊消息
     */
    IPage<WebSocketMessageDTO> getPrivateMessages(Long userId, Long targetUserId, Integer page, Integer size);
    
    /**
     * 获取用户未读消息
     */
    List<WebSocketMessageDTO> getUnreadMessages(Long userId);
    
    /**
     * 获取用户未读消息数
     */
    Integer getUnreadCount(Long userId);
    
    /**
     * 标记消息为已读
     */
    void markAsRead(Long messageId, Long userId);
    
    /**
     * 批量标记消息为已读
     */
    void markAsReadBatch(List<Long> messageIds, Long userId);
    
    /**
     * 标记房间所有消息为已读
     */
    void markRoomAsRead(String roomId, Long userId);
    
    /**
     * 查询消息列表（带条件）
     */
    IPage<WebSocketMessageDTO> queryMessages(MessageQueryRequest request);
    
    /**
     * 获取用户最近消息
     */
    List<WebSocketMessageDTO> getRecentMessages(Long userId, Integer limit);
}
