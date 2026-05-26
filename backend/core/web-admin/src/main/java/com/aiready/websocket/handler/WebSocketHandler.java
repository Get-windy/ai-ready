package com.aiready.websocket.handler;

import com.aiready.websocket.dto.WebSocketCommand;
import com.aiready.websocket.dto.WebSocketMessageDTO;
import com.aiready.websocket.dto.WebSocketResponse;
import com.aiready.websocket.service.ChatRoomService;
import com.aiready.websocket.service.WebSocketMessageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket处理器
 * 处理WebSocket连接、消息和断开
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {
    
    private final WebSocketMessageService messageService;
    private final ChatRoomService chatRoomService;
    private final ObjectMapper objectMapper;
    
    // 存储用户ID到Session的映射
    private static final Map<Long, WebSocketSession> USER_SESSION_MAP = new ConcurrentHashMap<>();
    
    // 存储房间ID到用户ID集合的映射
    private static final Map<String, Set<Long>> ROOM_USERS_MAP = new ConcurrentHashMap<>();
    
    // 存储用户心跳时间
    private static final Map<Long, LocalDateTime> USER_HEARTBEAT_MAP = new ConcurrentHashMap<>();
    
    // 心跳检测线程池
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // 心跳超时时间（毫秒）
    private static final long HEARTBEAT_TIMEOUT = 60000;
    
    public WebSocketHandler(WebSocketMessageService messageService, ChatRoomService chatRoomService) {
        this.messageService = messageService;
        this.chatRoomService = chatRoomService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        // 启动心跳检测
        startHeartbeatCheck();
    }
    
    /**
     * 连接建立后
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            log.warn("WebSocket连接未获取到用户ID，关闭连接");
            session.close();
            return;
        }
        
        // 存储用户会话
        USER_SESSION_MAP.put(userId, session);
        USER_HEARTBEAT_MAP.put(userId, LocalDateTime.now());
        
        log.info("WebSocket连接建立，用户ID: {}", userId);
        
        // 发送连接成功消息
        sendMessage(session, WebSocketResponse.success("CONNECT", "连接成功"));
        
        // 发送未读消息数
        Integer unreadCount = messageService.getUnreadCount(userId);
        sendMessage(session, WebSocketResponse.success("UNREAD_COUNT", unreadCount));
    }
    
    /**
     * 收到消息时
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId == null) {
            return;
        }
        
        String payload = message.getPayload();
        log.debug("收到WebSocket消息，用户ID: {}，内容: {}", userId, payload);
        
        try {
            WebSocketCommand command = objectMapper.readValue(payload, WebSocketCommand.class);
            handleCommand(session, userId, command);
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            sendMessage(session, WebSocketResponse.error("消息格式错误: " + e.getMessage()));
        }
    }
    
    /**
     * 处理命令
     */
    private void handleCommand(WebSocketSession session, Long userId, WebSocketCommand command) throws IOException {
        String cmd = command.getCommand();
        
        switch (cmd) {
            case "HEARTBEAT":
                handleHeartbeat(userId);
                sendMessage(session, WebSocketResponse.heartbeat());
                break;
            case "JOIN_ROOM":
                handleJoinRoom(session, userId, command.getRoomId());
                break;
            case "LEAVE_ROOM":
                handleLeaveRoom(userId, command.getRoomId());
                break;
            case "SEND_MESSAGE":
                handleSendMessage(session, userId, command);
                break;
            case "READ_MESSAGE":
                handleReadMessage(userId, command.getMessageId());
                break;
            case "GET_HISTORY":
                handleGetHistory(session, userId, command);
                break;
            default:
                sendMessage(session, WebSocketResponse.error("未知命令: " + cmd));
        }
    }
    
    /**
     * 处理心跳
     */
    private void handleHeartbeat(Long userId) {
        USER_HEARTBEAT_MAP.put(userId, LocalDateTime.now());
    }
    
    /**
     * 处理加入房间
     */
    private void handleJoinRoom(WebSocketSession session, Long userId, String roomId) throws IOException {
        if (roomId == null || roomId.isEmpty()) {
            sendMessage(session, WebSocketResponse.error("房间ID不能为空"));
            return;
        }
        
        // 检查用户是否在房间中
        if (!chatRoomService.isUserInRoom(roomId, userId)) {
            sendMessage(session, WebSocketResponse.error("您不在该房间中"));
            return;
        }
        
        // 添加到房间用户集合
        ROOM_USERS_MAP.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(userId);
        
        sendMessage(session, WebSocketResponse.success("JOIN_ROOM", "加入房间成功: " + roomId));
        log.info("用户 {} 加入房间 {}", userId, roomId);
    }
    
    /**
     * 处理离开房间
     */
    private void handleLeaveRoom(Long userId, String roomId) {
        if (roomId != null && !roomId.isEmpty()) {
            Set<Long> users = ROOM_USERS_MAP.get(roomId);
            if (users != null) {
                users.remove(userId);
            }
            log.info("用户 {} 离开房间 {}", userId, roomId);
        }
    }
    
    /**
     * 处理发送消息
     */
    private void handleSendMessage(WebSocketSession session, Long userId, WebSocketCommand command) throws IOException {
        // 构建发送消息请求
        com.aiready.websocket.dto.SendMessageRequest request = new com.aiready.websocket.dto.SendMessageRequest();
        request.setMessageType(command.getMessageType());
        request.setReceiverId(command.getTargetUserId());
        request.setRoomId(command.getRoomId());
        request.setContent(command.getContent());
        
        // 保存消息
        WebSocketMessageDTO message = messageService.sendMessage(userId, request);
        
        // 发送确认
        sendMessage(session, WebSocketResponse.ack(message.getMessageId()));
        
        // 转发消息
        if (command.getRoomId() != null && !command.getRoomId().isEmpty()) {
            // 群聊消息
            broadcastToRoom(command.getRoomId(), WebSocketResponse.success("MESSAGE", message), userId);
        } else if (command.getTargetUserId() != null) {
            // 私聊消息
            sendToUser(command.getTargetUserId(), WebSocketResponse.success("MESSAGE", message));
        }
    }
    
    /**
     * 处理标记已读
     */
    private void handleReadMessage(Long userId, String messageId) {
        if (messageId != null && !messageId.isEmpty()) {
            try {
                // 根据messageId查询消息并标记已读
                // 这里简化处理，实际应该通过messageId查询消息ID
                log.info("用户 {} 标记消息 {} 为已读", userId, messageId);
            } catch (Exception e) {
                log.error("标记消息已读失败", e);
            }
        }
    }
    
    /**
     * 处理获取历史消息
     */
    private void handleGetHistory(WebSocketSession session, Long userId, WebSocketCommand command) throws IOException {
        String roomId = command.getRoomId();
        Integer page = command.getPage() != null ? command.getPage() : 1;
        Integer size = command.getSize() != null ? command.getSize() : 20;
        
        com.baomidou.mybatisplus.core.metadata.IPage<WebSocketMessageDTO> messages;
        if (roomId != null && !roomId.isEmpty()) {
            messages = messageService.getRoomMessages(roomId, page, size);
        } else if (command.getTargetUserId() != null) {
            messages = messageService.getPrivateMessages(userId, command.getTargetUserId(), page, size);
        } else {
            sendMessage(session, WebSocketResponse.error("请指定房间ID或目标用户ID"));
            return;
        }
        
        sendMessage(session, WebSocketResponse.success("HISTORY", messages));
    }
    
    /**
     * 连接关闭后
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = getUserIdFromSession(session);
        if (userId != null) {
            // 从所有房间中移除
            ROOM_USERS_MAP.values().forEach(users -> users.remove(userId));
            
            // 移除会话
            USER_SESSION_MAP.remove(userId);
            USER_HEARTBEAT_MAP.remove(userId);
            
            log.info("WebSocket连接关闭，用户ID: {}，状态: {}", userId, status);
        }
    }
    
    /**
     * 发送消息给指定用户
     */
    public void sendToUser(Long userId, WebSocketResponse<?> response) {
        WebSocketSession session = USER_SESSION_MAP.get(userId);
        if (session != null && session.isOpen()) {
            sendMessage(session, response);
        }
    }
    
    /**
     * 广播消息给房间所有用户
     */
    public void broadcastToRoom(String roomId, WebSocketResponse<?> response, Long excludeUserId) {
        Set<Long> users = ROOM_USERS_MAP.get(roomId);
        if (users != null) {
            for (Long userId : users) {
                if (!userId.equals(excludeUserId)) {
                    sendToUser(userId, response);
                }
            }
        }
    }
    
    /**
     * 广播消息给所有在线用户
     */
    public void broadcastToAll(WebSocketResponse<?> response) {
        USER_SESSION_MAP.values().forEach(session -> sendMessage(session, response));
    }
    
    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, WebSocketResponse<?> response) {
        if (session != null && session.isOpen()) {
            try {
                String message = objectMapper.writeValueAsString(response);
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error("发送WebSocket消息失败", e);
            }
        }
    }
    
    /**
     * 从Session中获取用户ID
     */
    private Long getUserIdFromSession(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }
    
    /**
     * 启动心跳检测
     */
    private void startHeartbeatCheck() {
        heartbeatExecutor.scheduleAtFixedRate(() -> {
            LocalDateTime now = LocalDateTime.now();
            USER_HEARTBEAT_MAP.forEach((userId, lastHeartbeat) -> {
                if (java.time.Duration.between(lastHeartbeat, now).toMillis() > HEARTBEAT_TIMEOUT) {
                    log.warn("用户 {} 心跳超时，断开连接", userId);
                    WebSocketSession session = USER_SESSION_MAP.get(userId);
                    if (session != null) {
                        try {
                            session.close(CloseStatus.SESSION_NOT_RELIABLE);
                        } catch (IOException e) {
                            log.error("关闭超时连接失败", e);
                        }
                    }
                }
            });
        }, 30, 30, TimeUnit.SECONDS);
    }
    
    /**
     * 获取在线用户数量
     */
    public int getOnlineUserCount() {
        return USER_SESSION_MAP.size();
    }
    
    /**
     * 获取房间在线用户数量
     */
    public int getRoomOnlineCount(String roomId) {
        Set<Long> users = ROOM_USERS_MAP.get(roomId);
        return users != null ? users.size() : 0;
    }
}
