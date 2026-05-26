package cn.aiedge.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知模块配置属性
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    /**
     * 是否启用异步发送
     */
    private boolean asyncEnabled = true;

    /**
     * 异步发送线程池大小
     */
    private int asyncThreadPoolSize = 10;

    /**
     * 批量发送批次大小
     */
    private int batchSize = 100;

    /**
     * 最大重试次数
     */
    private int maxRetryCount = 3;

    /**
     * 重试间隔（毫秒）
     */
    private long retryIntervalMs = 5000;

    /**
     * 模板缓存是否启用
     */
    private boolean templateCacheEnabled = true;

    /**
     * 模板缓存过期时间（秒）
     */
    private long templateCacheExpireSeconds = 300;

    /**
     * 发送限流配置
     */
    private Map<String, RateLimitConfig> rateLimit = new HashMap<>();

    /**
     * 默认限流配置
     */
    public NotificationProperties() {
        // 默认邮件限流：每秒10封
        rateLimit.put("email", new RateLimitConfig(10, 1));
        // 默认短信限流：每秒5条
        rateLimit.put("sms", new RateLimitConfig(5, 1));
        // 站内信不限流
        rateLimit.put("site", new RateLimitConfig(Integer.MAX_VALUE, 1));
    }

    @Data
    public static class RateLimitConfig {
        /**
         * 令牌桶容量
         */
        private int permits;

        /**
         * 每秒补充令牌数
         */
        private double rate;

        public RateLimitConfig() {}

        public RateLimitConfig(int permits, double rate) {
            this.permits = permits;
            this.rate = rate;
        }
    }
}
