package cn.aiedge.base.config;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 配置变更监听器
 * 用于多实例间的配置同步
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigChangeListener implements MessageListener {

    private final RedisMessageListenerContainer redisContainer;
    private final StringRedisTemplate redisTemplate;
    
    // 本地配置缓存
    private static final Map<String, String> LOCAL_CACHE = new ConcurrentHashMap<>();
    
    private static final String CONFIG_CHANGE_CHANNEL = "sys:config:change";

    @PostConstruct
    public void init() {
        // 订阅配置变更频道
        redisContainer.addMessageListener(this, new PatternTopic(CONFIG_CHANGE_CHANNEL));
        log.info("配置变更监听器已启动，订阅频道: {}", CONFIG_CHANGE_CHANNEL);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            Map<String, Object> data = JSONUtil.toBean(json, Map.class);
            
            String key = (String) data.get("key");
            String value = (String) data.get("value");
            
            // 更新本地缓存
            if (value != null) {
                LOCAL_CACHE.put(key, value);
            } else {
                LOCAL_CACHE.remove(key);
            }
            
            log.info("收到配置变更通知: {} = {}", key, value);
            
            // 触发配置变更回调
            triggerCallbacks(key, value);
            
        } catch (Exception e) {
            log.error("处理配置变更消息失败: {}", e.getMessage());
        }
    }

    /**
     * 获取本地缓存的配置
     */
    public static String getCachedValue(String key) {
        return LOCAL_CACHE.get(key);
    }

    /**
     * 更新本地缓存
     */
    public static void updateCache(String key, String value) {
        if (value != null) {
            LOCAL_CACHE.put(key, value);
        } else {
            LOCAL_CACHE.remove(key);
        }
    }

    /**
     * 触发配置变更回调
     */
    private void triggerCallbacks(String key, String value) {
        // 可以在这里实现配置变更回调机制
        // 例如：通知相关Bean刷新配置
        log.debug("配置变更回调: {} = {}", key, value);
    }
}
