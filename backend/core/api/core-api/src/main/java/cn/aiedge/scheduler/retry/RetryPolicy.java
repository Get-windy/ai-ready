package cn.aiedge.scheduler.retry;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

/**
 * 重试策略配置
 * 
 * @author AI-Ready Team
 * @since 1.1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryPolicy {

    /**
     * 是否启用重试
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * 最大重试次数
     */
    @Builder.Default
    private int maxRetries = 3;

    /**
     * 重试间隔策略
     */
    @Builder.Default
    private RetryIntervalStrategy intervalStrategy = RetryIntervalStrategy.FIXED;

    /**
     * 固定间隔时间（毫秒）
     */
    @Builder.Default
    private long fixedIntervalMs = 5000;

    /**
     * 初始间隔时间（毫秒）- 用于指数退避
     */
    @Builder.Default
    private long initialIntervalMs = 1000;

    /**
     * 最大间隔时间（毫秒）
     */
    @Builder.Default
    private long maxIntervalMs = 60000;

    /**
     * 退避倍数（用于指数退避）
     */
    @Builder.Default
    private double multiplier = 2.0;

    /**
     * 重试异常类型（支持通配符）
     */
    @Builder.Default
    private String[] retryableExceptions = {"Exception"};

    /**
     * 排除异常类型
     */
    @Builder.Default
    private String[] excludedExceptions = {};

    /**
     * 重试间隔策略枚举
     */
    public enum RetryIntervalStrategy {
        /**
         * 固定间隔
         */
        FIXED,
        /**
         * 线性递增
         */
        LINEAR,
        /**
         * 指数退避
         */
        EXPONENTIAL,
        /**
         * 随机间隔
         */
        RANDOM
    }

    /**
     * 创建默认重试策略
     */
    public static RetryPolicy defaultPolicy() {
        return RetryPolicy.builder().build();
    }

    /**
     * 创建无重试策略
     */
    public static RetryPolicy noRetry() {
        return RetryPolicy.builder().enabled(false).build();
    }

    /**
     * 创建指数退避策略
     */
    public static RetryPolicy exponentialBackoff(int maxRetries, long initialIntervalMs, double multiplier) {
        return RetryPolicy.builder()
                .maxRetries(maxRetries)
                .intervalStrategy(RetryIntervalStrategy.EXPONENTIAL)
                .initialIntervalMs(initialIntervalMs)
                .multiplier(multiplier)
                .build();
    }

    /**
     * 计算下次重试间隔
     *
     * @param retryCount 当前重试次数
     * @return 间隔时间（毫秒）
     */
    public long calculateInterval(int retryCount) {
        return switch (intervalStrategy) {
            case FIXED -> fixedIntervalMs;
            case LINEAR -> Math.min(fixedIntervalMs * retryCount, maxIntervalMs);
            case EXPONENTIAL -> {
                long interval = (long) (initialIntervalMs * Math.pow(multiplier, retryCount - 1));
                yield Math.min(interval, maxIntervalMs);
            }
            case RANDOM -> {
                long min = fixedIntervalMs;
                long max = Math.min(fixedIntervalMs * 3, maxIntervalMs);
                yield min + (long) (Math.random() * (max - min));
            }
        };
    }

    /**
     * 检查异常是否可重试
     *
     * @param exception 异常
     * @return 是否可重试
     */
    public boolean isRetryable(Exception exception) {
        if (!enabled) return false;
        
        String exceptionName = exception.getClass().getName();
        
        // 检查排除列表
        for (String excluded : excludedExceptions) {
            if (matchesPattern(exceptionName, excluded)) {
                return false;
            }
        }
        
        // 检查可重试列表
        for (String retryable : retryableExceptions) {
            if (matchesPattern(exceptionName, retryable)) {
                return true;
            }
        }
        
        return false;
    }

    private boolean matchesPattern(String text, String pattern) {
        if (pattern.equals("*")) return true;
        if (pattern.endsWith("*")) {
            return text.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return text.equals(pattern) || text.endsWith("." + pattern);
    }
}
