package cn.aiedge.common.ratelimit;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 限流配置
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai-ready.rate-limit")
public class RateLimitConfig {

    // 手动添加 getter 方法，以防 @Data 不生效
    public boolean isEnabled() { return enabled; }
    public int getDefaultQps() { return defaultQps; }
    public int getDefaultCapacity() { return defaultCapacity; }
    public long getDefaultTimeout() { return defaultTimeout; }
    public int getIpQps() { return ipQps; }
    public int getUserQps() { return userQps; }
    public Map<String, ApiLimit> getApiLimits() { return apiLimits; }

    /**
     * 是否启用限流
     */
    private boolean enabled = true;

    /**
     * 默认每秒请求数
     */
    private int defaultQps = 100;

    /**
     * 默认令牌桶容量
     */
    private int defaultCapacity = 200;

    /**
     * 默认等待超时时间（毫秒）
     */
    private long defaultTimeout = 0L;

    /**
     * IP限流QPS
     */
    private int ipQps = 50;

    /**
     * 用户限流QPS
     */
    private int userQps = 100;

    /**
     * API级别限流配置
     */
    private Map<String, ApiLimit> apiLimits = new HashMap<>();

    /**
     * API限流配置
     */
    @Data
    public static class ApiLimit {
        /**
         * 每秒请求数
         */
        private int qps = 100;

        /**
         * 桶容量
         */
        private int capacity = 200;

        /**
         * 等待超时（毫秒）
         */
        private long timeout = 0;
    }
}
