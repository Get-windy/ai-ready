package cn.aiedge.mq.service;

import cn.aiedge.mq.config.RetryStrategy;
import cn.aiedge.mq.model.MessageEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 消息重试服务
 * 提供消息重试计数和延迟重试能力
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mq.rabbit", name = "enabled", havingValue = "true")
public class MessageRetryService {

    private final RetryStrategy retryStrategy;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String RETRY_COUNT_KEY = "mq:retry:count:";
    private static final String RETRY_TIME_KEY = "mq:retry:time:";

    /**
     * 记录重试次数
     *
     * @param messageId 消息ID
     * @return 当前重试次数
     */
    public int incrementRetryCount(String messageId) {
        String key = RETRY_COUNT_KEY + messageId;
        Long count = redisTemplate.opsForValue().increment(key);
        
        // 设置过期时间（24小时）
        if (count != null && count == 1) {
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
        }
        
        return count != null ? count.intValue() : 1;
    }

    /**
     * 获取重试次数
     *
     * @param messageId 消息ID
     * @return 重试次数
     */
    public int getRetryCount(String messageId) {
        String key = RETRY_COUNT_KEY + messageId;
        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? ((Number) count).intValue() : 0;
    }

    /**
     * 清除重试计数
     *
     * @param messageId 消息ID
     */
    public void clearRetryCount(String messageId) {
        String key = RETRY_COUNT_KEY + messageId;
        redisTemplate.delete(key);
    }

    /**
     * 判断是否可以重试
     *
     * @param message 消息实体
     * @return 是否可以重试
     */
    public boolean canRetry(MessageEntity message) {
        int currentCount = getRetryCount(message.getMessageId());
        int maxRetry = retryStrategy.getMaxRetry(message.getMessageType());
        return currentCount < maxRetry;
    }

    /**
     * 计算重试延迟时间
     *
     * @param message 消息实体
     * @return 延迟毫秒数
     */
    public long calculateRetryDelay(MessageEntity message) {
        int retryCount = getRetryCount(message.getMessageId());
        return retryStrategy.calculateDelay(retryCount, message.getMessageType());
    }

    /**
     * 设置下次重试时间
     *
     * @param messageId 消息ID
     * @param delayMillis 延迟毫秒数
     */
    public void scheduleRetry(String messageId, long delayMillis) {
        String key = RETRY_TIME_KEY + messageId;
        long retryTime = System.currentTimeMillis() + delayMillis;
        redisTemplate.opsForValue().set(key, retryTime, delayMillis + 60000, TimeUnit.MILLISECONDS);
        
        log.debug("消息重试已调度: messageId={}, delayMs={}, retryAt={}", 
            messageId, delayMillis, new java.util.Date(retryTime));
    }

    /**
     * 检查是否到达重试时间
     *
     * @param messageId 消息ID
     * @return 是否可以重试
     */
    public boolean isRetryTime(String messageId) {
        String key = RETRY_TIME_KEY + messageId;
        Object retryTime = redisTemplate.opsForValue().get(key);
        
        if (retryTime == null) {
            return true; // 没有调度记录，可以立即重试
        }
        
        return System.currentTimeMillis() >= ((Number) retryTime).longValue();
    }

    /**
     * 获取重试状态摘要
     *
     * @param message 消息实体
     * @return 重试状态信息
     */
    public RetryStatus getRetryStatus(MessageEntity message) {
        int currentCount = getRetryCount(message.getMessageId());
        int maxRetry = retryStrategy.getMaxRetry(message.getMessageType());
        long delay = calculateRetryDelay(message);
        
        return RetryStatus.builder()
            .messageId(message.getMessageId())
            .currentRetry(currentCount)
            .maxRetry(maxRetry)
            .canRetry(currentCount < maxRetry)
            .nextRetryDelay(delay)
            .build();
    }

    /**
     * 重试状态信息
     */
    @lombok.Data
    @lombok.Builder
    public static class RetryStatus {
        private String messageId;
        private int currentRetry;
        private int maxRetry;
        private boolean canRetry;
        private long nextRetryDelay;
    }
}
