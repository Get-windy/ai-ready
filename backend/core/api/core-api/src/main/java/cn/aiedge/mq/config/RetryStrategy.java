package cn.aiedge.mq.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 消息重试策略配置
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mq.retry")
public class RetryStrategy {

    /**
     * 默认最大重试次数
     */
    private int defaultMaxRetry = 3;

    /**
     * 重试间隔（毫秒）
     */
    private long retryInterval = 5000;

    /**
     * 是否启用指数退避
     */
    private boolean exponentialBackoff = true;

    /**
     * 初始退避时间（毫秒）
     */
    private long initialBackoff = 1000;

    /**
     * 最大退避时间（毫秒）
     */
    private long maxBackoff = 60000;

    /**
     * 退避乘数
     */
    private double backoffMultiplier = 2.0;

    /**
     * 按消息类型配置的重试策略
     */
    private Map<String, RetryConfig> typeConfigs = new HashMap<>();

    /**
     * 获取指定类型的最大重试次数
     */
    public int getMaxRetry(String messageType) {
        if (messageType == null) return defaultMaxRetry;
        
        RetryConfig config = typeConfigs.get(messageType);
        return config != null ? config.getMaxRetry() : defaultMaxRetry;
    }

    /**
     * 计算重试延迟时间
     * 
     * @param retryCount 当前重试次数
     * @param messageType 消息类型
     * @return 延迟毫秒数
     */
    public long calculateDelay(int retryCount, String messageType) {
        if (!exponentialBackoff) {
            return retryInterval;
        }

        RetryConfig config = typeConfigs.get(messageType);
        long baseDelay = config != null ? config.getInitialDelay() : initialBackoff;
        long maxDelay = config != null ? config.getMaxDelay() : maxBackoff;
        double multiplier = config != null ? config.getMultiplier() : backoffMultiplier;

        // 指数退避：delay = baseDelay * multiplier^retryCount
        long delay = (long) (baseDelay * Math.pow(multiplier, retryCount));
        
        // 添加随机抖动（避免惊群效应）
        delay = delay + (long) (Math.random() * 1000);

        return Math.min(delay, maxDelay);
    }

    /**
     * 单个消息类型的重试配置
     */
    @Data
    public static class RetryConfig {
        /**
         * 最大重试次数
         */
        private int maxRetry = 3;

        /**
         * 初始延迟
         */
        private long initialDelay = 1000;

        /**
         * 最大延迟
         */
        private long maxDelay = 60000;

        /**
         * 延迟乘数
         */
        private double multiplier = 2.0;
    }
}
