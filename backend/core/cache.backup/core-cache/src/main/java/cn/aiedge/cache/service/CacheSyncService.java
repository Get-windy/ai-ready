package cn.aiedge.cache.service;

import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 缓存同步服务
 * 基于Redis Pub/Sub实现分布式缓存同步
 *
 * @author AI-Ready Team
 * @since 2.0.0
 */
@Slf4j
@Service
public class CacheSyncService {

    private final StringRedisTemplate redisTemplate;
    private final RedisMessageListenerContainer container;
    private final ConcurrentHashMap<String, Consumer<CacheSyncMessage>> listeners = new ConcurrentHashMap<>();

    private static final String CACHE_SYNC_CHANNEL = "cache:sync:";

    public CacheSyncService(StringRedisTemplate redisTemplate, 
                           RedisMessageListenerContainer container) {
        this.redisTemplate = redisTemplate;
        this.container = container;
    }

    @PostConstruct
    public void init() {
        // 订阅所有缓存同步频道
        MessageListenerAdapter listenerAdapter = new MessageListenerAdapter(this, "onMessage");
        container.addMessageListener(listenerAdapter, new PatternTopic(CACHE_SYNC_CHANNEL + "*"));
        log.info("缓存同步服务初始化完成");
    }

    /**
     * 发布缓存同步消息
     */
    public void publishSync(String cacheName, String key, SyncAction action) {
        String channel = CACHE_SYNC_CHANNEL + cacheName;
        CacheSyncMessage message = new CacheSyncMessage();
        message.setCacheName(cacheName);
        message.setKey(key);
        message.setAction(action);
        message.setTimestamp(System.currentTimeMillis());
        message.setSourceNode(getNodeId());

        String json = JSONUtil.toJsonStr(message);
        redisTemplate.convertAndSend(channel, json);
        log.debug("发布缓存同步消息: channel={}, key={}, action={}", channel, key, action);
    }

    /**
     * 订阅缓存同步消息
     */
    public void subscribe(String cacheName, Consumer<CacheSyncMessage> listener) {
        listeners.put(cacheName, listener);
        log.info("订阅缓存同步: cacheName={}", cacheName);
    }

    /**
     * 取消订阅
     */
    public void unsubscribe(String cacheName) {
        listeners.remove(cacheName);
        log.info("取消订阅缓存同步: cacheName={}", cacheName);
    }

    /**
     * 接收Redis消息
     */
    public void onMessage(String message, String channel) {
        try {
            CacheSyncMessage syncMessage = JSONUtil.toBean(message, CacheSyncMessage.class);
            
            // 忽略自己发送的消息
            if (getNodeId().equals(syncMessage.getSourceNode())) {
                return;
            }

            log.debug("接收缓存同步消息: channel={}, key={}, action={}", 
                    channel, syncMessage.getKey(), syncMessage.getAction());

            // 通知监听器
            Consumer<CacheSyncMessage> listener = listeners.get(syncMessage.getCacheName());
            if (listener != null) {
                listener.accept(syncMessage);
            }
        } catch (Exception e) {
            log.error("处理缓存同步消息失败: {}", message, e);
        }
    }

    /**
     * 获取当前节点ID
     */
    private String getNodeId() {
        return System.getProperty("node.id", "default");
    }

    /**
     * 缓存同步消息
     */
    @Data
    public static class CacheSyncMessage {
        private String cacheName;
        private String key;
        private SyncAction action;
        private long timestamp;
        private String sourceNode;
    }

    /**
     * 同步动作枚举
     */
    public enum SyncAction {
        EVICT,      // 清除缓存
        UPDATE,     // 更新缓存
        CLEAR_ALL   // 清除所有
    }
}
