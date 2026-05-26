package com.aiready.websocket.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.aiready.websocket.dto.MessageQueryRequest;
import com.aiready.websocket.dto.SendMessageRequest;
import com.aiready.websocket.dto.WebSocketMessageDTO;
import com.aiready.websocket.entity.WebSocketMessage;
import com.aiready.websocket.mapper.WebSocketMessageMapper;
import com.aiready.websocket.service.WebSocketMessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * WebSocket消息服务实现
 */
@Service
@RequiredArgsConstructor
public class WebSocketMessageServiceImpl extends ServiceImpl<WebSocketMessageMapper, WebSocketMessage> 
        implements WebSocketMessageService {
    
    private final WebSocketMessageMapper messageMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebSocketMessageDTO sendMessage(Long senderId, SendMessageRequest request) {
        WebSocketMessage message = new WebSocketMessage();
        message.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        message.setMessageType(request.getMessageType());
        message.setSenderId(senderId);
        message.setReceiverId(request.getReceiverId());
        message.setRoomId(request.getRoomId());
        message.setContent(request.getContent());
        message.setContentType(request.getContentType());
        message.setExtraData(request.getExtraData());
        message.setBizType(request.getBizType());
        message.setBizId(request.getBizId());
        message.setReadStatus(0);
        message.setSendTime(LocalDateTime.now());
        
        messageMapper.insert(message);
        
        return convertToDTO(message);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebSocketMessageDTO sendSystemNotification(Long userId, String title, String content, String bizType, String bizId) {
        WebSocketMessage message = new WebSocketMessage();
        message.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        message.setMessageType(1); // 系统通知
        message.setSenderId(0L); // 系统发送
        message.setSenderName("系统");
        message.setReceiverId(userId);
        message.setContent(content);
        message.setContentType(1); // 文本
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setReadStatus(0);
        message.setSendTime(LocalDateTime.now());
        
        messageMapper.insert(message);
        
        return convertToDTO(message);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public WebSocketMessageDTO broadcastMessage(Long senderId, String content, String bizType) {
        WebSocketMessage message = new WebSocketMessage();
        message.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        message.setMessageType(5); // 广播消息
        message.setSenderId(senderId);
        message.setContent(content);
        message.setContentType(1);
        message.setBizType(bizType);
        message.setReadStatus(0);
        message.setSendTime(LocalDateTime.now());
        
        messageMapper.insert(message);
        
        return convertToDTO(message);
    }
    
    @Override
    public IPage<WebSocketMessageDTO> getRoomMessages(String roomId, Integer page, Integer size) {
        Page<WebSocketMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<WebSocketMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WebSocketMessage::getRoomId, roomId)
               .eq(WebSocketMessage::getDeleted, 0)
               .orderByDesc(WebSocketMessage::getCreateTime);
        
        IPage<WebSocketMessage> messagePage = messageMapper.selectPage(pageParam, wrapper);
        
        return messagePage.convert(this::convertToDTO);
    }
    
    @Override
    public IPage<WebSocketMessageDTO> getPrivateMessages(Long userId, Long targetUserId, Integer page, Integer size) {
        Page<WebSocketMessage> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<WebSocketMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WebSocketMessage::getMessageType, 3) // 私聊消息
               .and(w -> w.eq(WebSocketMessage::getSenderId, userId).eq(WebSocketMessage::getReceiverId, targetUserId)
                          .or()
                          .eq(WebSocketMessage::getSenderId, targetUserId).eq(WebSocketMessage::getReceiverId, userId))
               .eq(WebSocketMessage::getDeleted, 0)
               .orderByDesc(WebSocketMessage::getCreateTime);
        
        IPage<WebSocketMessage> messagePage = messageMapper.selectPage(pageParam, wrapper);
        
        return messagePage.convert(this::convertToDTO);
    }
    
    @Override
    public List<WebSocketMessageDTO> getUnreadMessages(Long userId) {
        List<WebSocketMessage> messages = messageMapper.selectUnreadMessages(userId);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Override
    public Integer getUnreadCount(Long userId) {
        return messageMapper.countUnreadMessages(userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long messageId, Long userId) {
        messageMapper.markAsRead(messageId, userId, LocalDateTime.now());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsReadBatch(List<Long> messageIds, Long userId) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        String idsStr = messageIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        messageMapper.markAsReadBatch(idsStr, userId, LocalDateTime.now());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRoomAsRead(String roomId, Long userId) {
        messageMapper.markRoomMessagesAsRead(roomId, userId, LocalDateTime.now());
    }
    
    @Override
    public IPage<WebSocketMessageDTO> queryMessages(MessageQueryRequest request) {
        Page<WebSocketMessage> pageParam = new Page<>(request.getPage(), request.getSize());
        LambdaQueryWrapper<WebSocketMessage> wrapper = new LambdaQueryWrapper<>();
        
        if (request.getRoomId() != null) {
            wrapper.eq(WebSocketMessage::getRoomId, request.getRoomId());
        }
        if (request.getSenderId() != null) {
            wrapper.eq(WebSocketMessage::getSenderId, request.getSenderId());
        }
        if (request.getReceiverId() != null) {
            wrapper.eq(WebSocketMessage::getReceiverId, request.getReceiverId());
        }
        if (request.getMessageType() != null) {
            wrapper.eq(WebSocketMessage::getMessageType, request.getMessageType());
        }
        if (request.getBizType() != null) {
            wrapper.eq(WebSocketMessage::getBizType, request.getBizType());
        }
        if (request.getReadStatus() != null) {
            wrapper.eq(WebSocketMessage::getReadStatus, request.getReadStatus());
        }
        if (request.getStartTime() != null) {
            wrapper.ge(WebSocketMessage::getCreateTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.le(WebSocketMessage::getCreateTime, request.getEndTime());
        }
        wrapper.eq(WebSocketMessage::getDeleted, 0);
        wrapper.orderByDesc(WebSocketMessage::getCreateTime);
        
        IPage<WebSocketMessage> messagePage = messageMapper.selectPage(pageParam, wrapper);
        return messagePage.convert(this::convertToDTO);
    }
    
    @Override
    public List<WebSocketMessageDTO> getRecentMessages(Long userId, Integer limit) {
        List<WebSocketMessage> messages = messageMapper.selectRecentMessages(userId, limit);
        return messages.stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    private WebSocketMessageDTO convertToDTO(WebSocketMessage message) {
        WebSocketMessageDTO dto = new WebSocketMessageDTO();
        BeanUtil.copyProperties(message, dto);
        return dto;
    }
}
