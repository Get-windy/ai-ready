package cn.aiedge.base.config.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Redis 缓存健康检查指示器
 * 
 * 检查项：
 * 1. Redis 连接状态
 * 2. Redis 内存使用情况
 * 3. Redis 响应时间
 * 4. 连接池活跃连接数
 * 
 * @author devops-engineer
 * @since 1.0.0
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisHealthIndicator(RedisConnectionFactory redisConnectionFactory, 
                                 RedisTemplate<String, Object> redisTemplate) {
        this.redisConnectionFactory = redisConnectionFactory;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            // 测试 PING 命令响应时间
            long startTime = System.currentTimeMillis();
            String pingResult = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();
            long pingTime = System.currentTimeMillis() - startTime;

            if (!"PONG".equalsIgnoreCase(pingResult)) {
                return Health.down()
                        .withDetail("error", "Redis PING failed: " + pingResult)
                        .build();
            }

            Health.Builder builder = Health.up()
                    .withDetail("redis", "connected")
                    .withDetail("pingTimeMs", pingTime);

            // 获取 Redis INFO 信息
            try {
                Properties info = redisTemplate.getConnectionFactory()
                        .getConnection()
                        .info();
                
                // 内存使用信息
                String usedMemory = info.getProperty("used_memory_human", "unknown");
                String maxMemory = info.getProperty("maxmemory_human", "0B");
                String memoryRatio = calculateMemoryRatio(info);
                
                builder.withDetail("usedMemory", usedMemory)
                       .withDetail("maxMemory", maxMemory)
                       .withDetail("memoryRatio", memoryRatio);

                // 连接信息
                String connectedClients = info.getProperty("connected_clients", "0");
                String blockedClients = info.getProperty("blocked_clients", "0");
                builder.withDetail("connectedClients", connectedClients)
                       .withDetail("blockedClients", blockedClients);

                // Redis 版本
                String redisVersion = info.getProperty("redis_version", "unknown");
                builder.withDetail("version", redisVersion);

                // 内存警告阈值
                if (memoryRatio != null && !memoryRatio.equals("unknown")) {
                    double ratio = Double.parseDouble(memoryRatio.replace("%", ""));
                    if (ratio > 80) {
                        builder.withDetail("warning", "Redis memory usage is high: " + memoryRatio);
                    }
                }

                // 响应时间警告
                if (pingTime > 50) {
                    builder.withDetail("warning", "Redis response time is slow");
                }

            } catch (Exception infoEx) {
                builder.withDetail("infoError", "Could not retrieve Redis INFO: " + infoEx.getMessage());
            }

            return builder.build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("redis", "connection failed")
                    .withException(e)
                    .build();
        }
    }

    /**
     * 计算内存使用比例
     */
    private String calculateMemoryRatio(Properties info) {
        try {
            long usedMemory = Long.parseLong(info.getProperty("used_memory", "0"));
            long maxMemory = Long.parseLong(info.getProperty("maxmemory", "0"));
            
            if (maxMemory == 0) {
                return "unknown (no max_memory set)";
            }
            
            double ratio = (usedMemory * 100.0) / maxMemory;
            return String.format("%.2f%%", ratio);
            
        } catch (NumberFormatException e) {
            return "unknown";
        }
    }
}