package cn.aiedge.base.config.health;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
@ConditionalOnClass(name = "org.springframework.data.redis.connection.RedisConnectionFactory")
public class RedisHealthIndicator implements HealthIndicator {

    @Autowired(required = false)
    private RedisConnectionFactory redisConnectionFactory;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        if (redisConnectionFactory == null || redisTemplate == null) {
            return Health.unknown()
                    .withDetail("redis", "not configured")
                    .build();
        }
        
        try {
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

            try {
                Properties info = redisTemplate.getConnectionFactory()
                        .getConnection()
                        .info();
                
                String usedMemory = info.getProperty("used_memory_human", "unknown");
                String maxMemory = info.getProperty("maxmemory_human", "0B");
                String memoryRatio = calculateMemoryRatio(info);
                
                builder.withDetail("usedMemory", usedMemory)
                       .withDetail("maxMemory", maxMemory)
                       .withDetail("memoryRatio", memoryRatio);

                String connectedClients = info.getProperty("connected_clients", "0");
                String blockedClients = info.getProperty("blocked_clients", "0");
                builder.withDetail("connectedClients", connectedClients)
                       .withDetail("blockedClients", blockedClients);

                String redisVersion = info.getProperty("redis_version", "unknown");
                builder.withDetail("version", redisVersion);

                if (memoryRatio != null && !memoryRatio.equals("unknown")) {
                    double ratio = Double.parseDouble(memoryRatio.replace("%", ""));
                    if (ratio > 80) {
                        builder.withDetail("warning", "Redis memory usage is high: " + memoryRatio);
                    }
                }

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