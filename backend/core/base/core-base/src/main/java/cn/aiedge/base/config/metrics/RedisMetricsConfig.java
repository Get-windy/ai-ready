package cn.aiedge.base.config.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RedisMetricsConfig {

    public static final String REDIS_CONNECTION_STATUS = "ai_ready_redis_connection_status";
    public static final String REDIS_MEMORY_USED = "ai_ready_redis_memory_used_bytes";
    public static final String REDIS_MEMORY_MAX = "ai_ready_redis_memory_max_bytes";
    public static final String REDIS_CONNECTED_CLIENTS = "ai_ready_redis_connected_clients";
    public static final String REDIS_KEY_COUNT = "ai_ready_redis_key_count";
    public static final String REDIS_RESPONSE_TIME = "ai_ready_redis_response_time_ms";
    public static final String REDIS_HIT_COUNT = "ai_ready_redis_hit_total";
    public static final String REDIS_MISS_COUNT = "ai_ready_redis_miss_total";

    @Autowired(required = false)
    private RedisConnectionFactory connectionFactory;
    
    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    private final MeterRegistry meterRegistry;
    
    private final AtomicLong lastResponseTime = new AtomicLong(0);
    private final Counter hitCounter;
    private final Counter missCounter;

    public RedisMetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        this.hitCounter = Counter.builder(REDIS_HIT_COUNT)
                .description("Redis cache hit count")
                .register(meterRegistry);
        this.missCounter = Counter.builder(REDIS_MISS_COUNT)
                .description("Redis cache miss count")
                .register(meterRegistry);
    }

    public void registerGauges() {
        if (connectionFactory == null) {
            return;
        }
        
        Gauge.builder(REDIS_CONNECTION_STATUS, () -> {
            try {
                String ping = connectionFactory.getConnection().ping();
                return "PONG".equalsIgnoreCase(ping) ? 1 : 0;
            } catch (Exception e) {
                return 0;
            }
        })
                .description("Redis connection status (1=connected, 0=disconnected)")
                .register(meterRegistry);

        Gauge.builder(REDIS_MEMORY_USED, () -> {
            try {
                Properties info = connectionFactory.getConnection().info("memory");
                return Long.parseLong(info.getProperty("used_memory", "0"));
            } catch (Exception e) {
                return 0L;
            }
        })
                .description("Redis memory used in bytes")
                .register(meterRegistry);

        Gauge.builder(REDIS_MEMORY_MAX, () -> {
            try {
                Properties info = connectionFactory.getConnection().info("memory");
                return Long.parseLong(info.getProperty("maxmemory", "0"));
            } catch (Exception e) {
                return 0L;
            }
        })
                .description("Redis max memory in bytes")
                .register(meterRegistry);

        Gauge.builder(REDIS_CONNECTED_CLIENTS, () -> {
            try {
                Properties info = connectionFactory.getConnection().info("clients");
                return Integer.parseInt(info.getProperty("connected_clients", "0"));
            } catch (Exception e) {
                return 0;
            }
        })
                .description("Number of connected Redis clients")
                .register(meterRegistry);

        Gauge.builder(REDIS_KEY_COUNT, () -> {
            try {
                Object result = connectionFactory.getConnection()
                        .execute("DBSIZE", new byte[0]);
                Long keyCount = result != null ? Long.valueOf(result.toString()) : 0L;
                return keyCount != null ? keyCount : 0L;
            } catch (Exception e) {
                return 0L;
            }
        })
                .description("Total number of keys in Redis")
                .register(meterRegistry);

        Gauge.builder(REDIS_RESPONSE_TIME, lastResponseTime::get)
                .description("Redis response time in milliseconds")
                .register(meterRegistry);
    }

    public void recordHit() {
        hitCounter.increment();
    }

    public void recordMiss() {
        missCounter.increment();
    }

    public void updateResponseTime(long responseTimeMs) {
        lastResponseTime.set(responseTimeMs);
    }
}