package cn.aiedge.cache.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 测试环境缓存配置
 */
@TestConfiguration
public class TestCacheConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        return container;
    }

    @Bean(name = "cacheExecutor")
    public Executor cacheExecutor() {
        return Executors.newFixedThreadPool(4, r -> new Thread(r, "cache-worker"));
    }
}