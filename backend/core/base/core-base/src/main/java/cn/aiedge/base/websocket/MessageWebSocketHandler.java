package cn.aiedge.base.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息WebSocket处理器
 * 用于站内信实时推送
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class MessageWebSocketHandler extends TextWebSocketHandler {

    /**
     * 用户WebSocket会话映射 key: userId
     */
    private static final ConcurrentHashMap<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.put(userId, session);
            log.info("WebSocket连接建立: userId={}", userId);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("收到WebSocket消息: {}", payload);
        // 处理心跳等消息
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        if (userId != null) {
            userSessions.remove(userId);
            log.info("WebSocket连接关闭: userId={}", userId);
        }
    }

    /**
     * 发送消息给指定用户
     */
    public void sendMessageToUser(String userId, String message) {
        WebSocketSession session = userSessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.debug("WebSocket消息发送成功: userId={}", userId);
            } catch (IOException e) {
                log.error("WebSocket消息发送失败: userId={}", userId, e);
            }
        } else {
            log.debug("用户不在线，消息未发送: userId={}", userId);
        }
    }

    /**
     * 广播消息给所有在线用户
     */
    public void broadcastMessage(String message) {
        userSessions.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    log.error("广播消息发送失败: userId={}", userId, e);
                }
            }
        });
    }

    /**
     * 从session中获取用户ID
     */
    private String getUserIdFromSession(WebSocketSession session) {
        // 从session属性中获取用户ID
        Object userId = session.getAttributes().get("userId");
        return userId != null ? userId.toString() : null;
    }

    /**
     * 获取在线用户数量
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }
}
