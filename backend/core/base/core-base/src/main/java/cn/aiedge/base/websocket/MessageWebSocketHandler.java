package cn.aiedge.base.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 消息 WebSocket 处理器
 *
 * 处理实时通知推送和基础聊天功能，与前端 useNotification / useWebSocket 协作。
 *
 * 支持的协议命令：
 * - HEARTBEAT       心跳保活
 * - JOIN_ROOM       加入消息房间
 * - LEAVE_ROOM      离开消息房间
 * - SEND_MESSAGE    发送消息
 * - READ_MESSAGE    标记已读
 * - GET_HISTORY     获取历史消息
 */
@Slf4j
@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    /** userId → WebSocketSession */
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    /** roomId → Set<userId> */
    private final Map<String, Set<Long>> roomUsers = new ConcurrentHashMap<>();

    /** userId → 最后一次心跳时间 */
    private final Map<Long, LocalDateTime> heartbeats = new ConcurrentHashMap<>();

    private static final long HEARTBEAT_TIMEOUT_SECONDS = 90;
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();

    public MessageWebSocketHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
        // 每 30 秒检查一次心跳超时
        heartbeatExecutor.scheduleAtFixedRate(this::checkHeartbeats, 30, 30, TimeUnit.SECONDS);
    }

    // ── 连接生命周期 ────────────────────────────────────────

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = getUserId(session);
        if (userId == null) {
            closeQuietly(session);
            return;
        }

        userSessions.put(userId, session);
        heartbeats.put(userId, LocalDateTime.now());

        log.info("WebSocket 连接建立，用户ID: {}", userId);

        // 发送 CONNECT 事件
        sendJson(session, WebSocketResponse.success("CONNECT", userId));

        // 发送 UNREAD_COUNT（当前简化：无数据库查询，固定返回0）
        sendJson(session, WebSocketResponse.success("UNREAD_COUNT", Map.of("count", 0)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = getUserId(session);
        if (userId == null) return;

        String payload = message.getPayload();
        log.debug("收到 WebSocket 消息，用户ID: {}，内容: {}", userId, payload);

        try {
            WebSocketCommand command = objectMapper.readValue(payload, WebSocketCommand.class);
            dispatchCommand(session, userId, command);
        } catch (Exception e) {
            log.error("解析 WebSocket 命令失败", e);
            sendJson(session, WebSocketResponse.error("命令格式错误"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = getUserId(session);
        if (userId == null) return;

        // 从所有房间中移除
        roomUsers.values().forEach(users -> users.remove(userId));

        userSessions.remove(userId);
        heartbeats.remove(userId);

        log.info("WebSocket 连接关闭，用户ID: {}，状态: {}", userId, status);
    }

    // ── 命令分发 ────────────────────────────────────────────

    private void dispatchCommand(WebSocketSession session, Long userId, WebSocketCommand cmd) throws IOException {
        String action = cmd.getCommand();
        if (action == null) {
            sendJson(session, WebSocketResponse.error("缺少 command 字段"));
            return;
        }

        switch (action.toUpperCase()) {
            case "HEARTBEAT":
                handleHeartbeat(session, userId);
                break;
            case "JOIN_ROOM":
                handleJoinRoom(session, userId, cmd.getRoomId());
                break;
            case "LEAVE_ROOM":
                handleLeaveRoom(userId, cmd.getRoomId());
                break;
            case "SEND_MESSAGE":
                sendJson(session, WebSocketResponse.error("服务端暂不支持直接发送消息"));
                break;
            case "READ_MESSAGE":
                handleReadMessage(session, userId, cmd);
                break;
            case "GET_HISTORY":
                sendJson(session, WebSocketResponse.error("历史消息功能暂未实现"));
                break;
            default:
                sendJson(session, WebSocketResponse.error("未知命令: " + action));
        }
    }

    // ── 命令处理 ────────────────────────────────────────────

    private void handleHeartbeat(WebSocketSession session, Long userId) {
        heartbeats.put(userId, LocalDateTime.now());
        sendJson(session, WebSocketResponse.heartbeat());
    }

    private void handleJoinRoom(WebSocketSession session, Long userId, String roomId) throws IOException {
        if (roomId == null || roomId.isEmpty()) {
            sendJson(session, WebSocketResponse.error("roomId 不能为空"));
            return;
        }
        roomUsers.computeIfAbsent(roomId, k -> new CopyOnWriteArraySet<>()).add(userId);
        sendJson(session, WebSocketResponse.success("JOIN_ROOM", "已加入房间: " + roomId));
        log.info("用户 {} 加入房间 {}", userId, roomId);
    }

    private void handleLeaveRoom(Long userId, String roomId) {
        if (roomId != null) {
            Set<Long> users = roomUsers.get(roomId);
            if (users != null) {
                users.remove(userId);
            }
        }
    }

    private void handleReadMessage(WebSocketSession session, Long userId, WebSocketCommand cmd) {
        String messageId = cmd.getMessageId();
        log.info("用户 {} 标记消息 {} 为已读", userId, messageId);
        sendJson(session, WebSocketResponse.ack(messageId));
    }

    // ── 心跳超时检查 ────────────────────────────────────────

    private void checkHeartbeats() {
        LocalDateTime now = LocalDateTime.now();
        heartbeats.forEach((userId, lastBeat) -> {
            if (ChronoUnit.SECONDS.between(lastBeat, now) > HEARTBEAT_TIMEOUT_SECONDS) {
                log.warn("用户 {} 心跳超时，断开连接", userId);
                WebSocketSession session = userSessions.get(userId);
                if (session != null && session.isOpen()) {
                    closeQuietly(session);
                }
            }
        });
    }

    // ── 对外推送接口 ────────────────────────────────────────

    /** 发送消息给指定用户 */
    public void sendToUser(Long userId, WebSocketResponse<?> response) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            sendJson(session, response);
        }
    }

    /** 广播给房间内所有用户 */
    public void broadcastToRoom(String roomId, WebSocketResponse<?> response, Long excludeUserId) {
        Set<Long> users = roomUsers.get(roomId);
        if (users != null) {
            for (Long uid : users) {
                if (!uid.equals(excludeUserId)) {
                    sendToUser(uid, response);
                }
            }
        }
    }

    /** 获取在线用户数 */
    public int getOnlineCount() {
        return userSessions.size();
    }

    /** 获取房间在线数 */
    public int getRoomOnlineCount(String roomId) {
        Set<Long> users = roomUsers.get(roomId);
        return users != null ? users.size() : 0;
    }

    // ── 工具方法 ────────────────────────────────────────────

    private Long getUserId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    private void sendJson(WebSocketSession session, Object data) {
        if (session == null || !session.isOpen()) return;
        try {
            String json = objectMapper.writeValueAsString(data);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("发送 WebSocket 消息失败", e);
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException e) {
            log.warn("关闭WebSocket连接失败", e);
        }
    }
}
