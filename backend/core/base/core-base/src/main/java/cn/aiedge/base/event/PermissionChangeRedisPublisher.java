package cn.aiedge.base.event;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 权限变更事件监听器（仅负责 Redis 广播）
 *
 * 监听本实例的 Spring {@link PermissionChangeEvent}，通过 Redis Pub/Sub 广播给所有实例。
 * SSE 推送由 {@link PermissionChangeRedisListener} 统一处理（各实例收到 Redis 消息后自行推送），
 * 避免同一实例上的重复推送。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PermissionChangeRedisPublisher {

    private final StringRedisTemplate redisTemplate;

    /** Redis Pub/Sub 频道名称 */
    public static final String PERMISSION_CHANGE_CHANNEL = "system:permission:change";

    /**
     * 监听权限变更事件，通过 Redis Pub/Sub 广播给所有实例
     */
    @Async
    @EventListener
    public void onPermissionChange(PermissionChangeEvent event) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("changeType", event.getChangeType().name());
            message.put("tenantId", event.getTenantId());
            message.put("description", event.getDescription());
            message.put("timestamp", System.currentTimeMillis());

            if (event.getAffectedUserIds() != null) {
                message.put("affectedUserIds", event.getAffectedUserIds());
                message.put("broadcast", false);
            } else {
                message.put("broadcast", true);
            }

            String jsonMessage = JSONUtil.toJsonStr(message);
            redisTemplate.convertAndSend(PERMISSION_CHANGE_CHANNEL, jsonMessage);

            log.info("[权限变更] 已广播到Redis频道: type={}, desc={}",
                    event.getChangeType(), event.getDescription());
        } catch (Exception e) {
            log.error("[权限变更] 广播事件失败: {}", e.getMessage(), e);
        }
    }
}
