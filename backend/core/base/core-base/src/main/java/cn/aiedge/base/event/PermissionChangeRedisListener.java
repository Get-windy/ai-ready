package cn.aiedge.base.event;

import cn.aiedge.base.websocket.SseNotificationService;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Redis Pub/Sub 权限变更消息监听器
 * 接收来自其他实例的权限变更通知，清除本地缓存并推送 SSE
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionChangeRedisListener implements MessageListener {

    private final StringRedisTemplate redisTemplate;
    private final SseNotificationService sseService;
    private final UnifiedPermissionCacheEvictor cacheEvictor;

    @PostConstruct
    public void init() {
        // 订阅权限变更频道
        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            connection.subscribe(this,
                    PermissionChangeRedisPublisher.PERMISSION_CHANGE_CHANNEL.getBytes());
            return null;
        });
        log.info("[权限变更] 已订阅 Redis 频道: {}", PermissionChangeRedisPublisher.PERMISSION_CHANGE_CHANNEL);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String body = new String(message.getBody());
            JSONObject json = JSONUtil.parseObj(body);

            String changeType = json.getStr("changeType");
            String description = json.getStr("description");
            boolean isBroadcast = json.getBool("broadcast", true);

            log.info("[权限变更] 收到Redis消息: type={}, broadcast={}, desc={}",
                    changeType, isBroadcast, description);

            // 1. 清除本地缓存
            if (isBroadcast) {
                cacheEvictor.evictAll();
                // 广播 SSE 给所有连接
                sseService.broadcast("permission-change", body);
            } else {
                // 定向清除缓存
                var affectedUserIds = json.getJSONArray("affectedUserIds");
                if (affectedUserIds != null) {
                    for (int i = 0; i < affectedUserIds.size(); i++) {
                        Long userId = affectedUserIds.getLong(i);
                        cacheEvictor.evictUser(userId);
                        sseService.sendToUser(userId, "permission-change", body);
                    }
                }
            }

        } catch (Exception e) {
            log.error("[权限变更] 处理Redis消息失败: {}", e.getMessage(), e);
        }
    }
}
