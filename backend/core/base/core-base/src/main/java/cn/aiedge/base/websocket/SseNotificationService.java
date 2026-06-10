package cn.aiedge.base.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 推送服务
 * <p>
 * 管理 SseEmitter 连接，提供向指定用户或所有用户推送事件的能力。
 * 主要用于缓存失效通知、权限变更通知等场景。
 * </p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class SseNotificationService {

    /** userId → SseEmitter 映射 */
    private final Map<Long, SseEmitter> userEmitters = new ConcurrentHashMap<>();

    /** 默认超时时间：30 分钟 */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /**
     * 为指定用户创建 SSE 连接
     */
    public SseEmitter createEmitter(Long userId) {
        // 如果已有旧连接，先完成它
        SseEmitter existing = userEmitters.remove(userId);
        if (existing != null) {
            existing.complete();
        }

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        userEmitters.put(userId, emitter);

        emitter.onCompletion(() -> {
            userEmitters.remove(userId);
            log.debug("SSE 连接完成: userId={}", userId);
        });

        emitter.onTimeout(() -> {
            userEmitters.remove(userId);
            log.debug("SSE 连接超时: userId={}", userId);
        });

        emitter.onError(e -> {
            userEmitters.remove(userId);
            log.debug("SSE 连接异常: userId={}, error={}", userId, e.getMessage());
        });

        // 发送初始连接确认事件
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"userId\":" + userId + ",\"message\":\"SSE连接已建立\"}"));
        } catch (IOException e) {
            userEmitters.remove(userId);
            log.warn("SSE 发送连接确认失败: userId={}", userId);
        }

        log.info("SSE 连接建立: userId={}, 当前连接数={}", userId, userEmitters.size());
        return emitter;
    }

    /**
     * 向指定用户推送事件
     */
    public boolean sendToUser(Long userId, String eventName, String data) {
        SseEmitter emitter = userEmitters.get(userId);
        if (emitter == null) {
            log.debug("用户 {} 无活跃 SSE 连接，跳过推送", userId);
            return false;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            return true;
        } catch (IOException e) {
            userEmitters.remove(userId);
            log.debug("SSE 发送失败，已移除连接: userId={}", userId);
            return false;
        }
    }

    /**
     * 向所有连接广播事件
     */
    public void broadcast(String eventName, String data) {
        if (userEmitters.isEmpty()) {
            return;
        }

        for (Map.Entry<Long, SseEmitter> entry : userEmitters.entrySet()) {
            try {
                entry.getValue().send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                userEmitters.remove(entry.getKey());
                log.debug("SSE 广播失败，已移除连接: userId={}", entry.getKey());
            }
        }
    }

    /**
     * 推送缓存失效通知给指定用户
     */
    public void notifyCacheInvalidation(Long userId) {
        boolean sent = sendToUser(userId, "cache-invalidate",
                "{\"type\":\"permission\",\"userId\":" + userId + "}");
        if (sent) {
            log.info("已通过 SSE 通知用户 {} 权限缓存失效", userId);
        }
    }

    /**
     * 获取当前活跃连接数
     */
    public int getActiveConnectionCount() {
        return userEmitters.size();
    }

    /**
     * 断开指定用户的 SSE 连接
     */
    public void disconnect(Long userId) {
        SseEmitter emitter = userEmitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE 连接已主动断开: userId={}", userId);
        }
    }
}
