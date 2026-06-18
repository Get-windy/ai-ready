package cn.aiedge.common.rate;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 限流配置类
 * 
 * 定义限流相关的配置参数
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "aiedge.rate-limit")
public class RateLimitConfig {
    
    /**
     * 是否启用限流
     */
    private boolean enabled = true;
    
    /**
     * 默认限流规则
     */
    private Rule defaultRule = new Rule();
    
    /**
     * 配置描述
     */
    private String description = "Default Rate Limit Configuration";
    
    /**
     * 限流规则配置
     */
    @Data
    public static class Rule {
        /**
         * 限制的请求数
         */
        private long limit = 100;
        
        /**
         * 时间窗口（秒）
         */
        private long window = 60;
        
        /**
         * 限流算法类型：token_bucket（令牌桶）, leaky_bucket（漏桶）, sliding_window（滑动窗口）
         */
        private Algorithm algorithm = Algorithm.TOKEN_BUCKET;
        
        /**
         * 令牌桶容量
         */
        private long capacity = 100;
        
        /**
         * 令牌生成速率（每秒）
         */
        private long refillRate = 10;
        
        /**
         * 配置描述
         */
        private String description = "Default Rate Limit Config";
        
        // 手动添加 getter 方法以确保编译成功
        public long getLimit() { return limit; }
        public long getWindow() { return window; }
        public Algorithm getAlgorithm() { return algorithm; }
        public long getCapacity() { return capacity; }
        public long getRefillRate() { return refillRate; }
        public String getDescription() { return description; }
    }
    
    /**
     * 限流算法枚举
     */
    public enum Algorithm {
        TOKEN_BUCKET,    // 令牌桶算法
        LEAKY_BUCKET,    // 漏桶算法
        SLIDING_WINDOW   // 滑动窗口算法
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
}