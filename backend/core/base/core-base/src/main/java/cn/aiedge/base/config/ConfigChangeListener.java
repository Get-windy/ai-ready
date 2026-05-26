package cn.aiedge.base.config;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class ConfigChangeListener implements MessageListener {

    @Autowired(required = false)
    private RedisMessageListenerContainer redisContainer;
    
    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;
    
    private static final Map<String, String> LOCAL_CACHE = new ConcurrentHashMap<>();
    
    private static final String CONFIG_CHANGE_CHANNEL = "sys:config:change";

    @PostConstruct
    public void init() {
        if (redisContainer != null) {
            redisContainer.addMessageListener(this, new PatternTopic(CONFIG_CHANGE_CHANNEL));
            log.info("配置变更监听器已启动，订阅频道: {}", CONFIG_CHANGE_CHANNEL);
        } else {
            log.info("Redis未配置，配置变更监听器以本地模式运行");
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            Map<String, Object> data = JSONUtil.toBean(json, Map.class);
            
            String key = (String) data.get("key");
            String value = (String) data.get("value");
            
            if (value != null) {
                LOCAL_CACHE.put(key, value);
            } else {
                LOCAL_CACHE.remove(key);
            }
            
            log.info("收到配置变更通知: {} = {}", key, value);
            triggerCallbacks(key, value);
            
        } catch (Exception e) {
            log.error("处理配置变更消息失败: {}", e.getMessage());
        }
    }

    public static String getCachedValue(String key) {
        return LOCAL_CACHE.get(key);
    }

    public static void updateCache(String key, String value) {
        if (value != null) {
            LOCAL_CACHE.put(key, value);
        } else {
            LOCAL_CACHE.remove(key);
        }
    }

    private void triggerCallbacks(String key, String value) {
        log.debug("配置变更回调: {} = {}", key, value);
    }
}