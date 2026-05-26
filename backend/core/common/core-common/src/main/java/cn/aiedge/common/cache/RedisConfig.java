package cn.aiedge.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis配置类
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * 配置StringRedisTemplate
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        
        // 使用String序列化器
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        log.info("Redis配置完成");
        return template;
    }

    /**
     * 注册RedisCache Bean
     */
    @Bean
    public RedisCache redisCache(StringRedisTemplate template) {
        return new RedisCache(template);
    }

    /**
     * 注册CacheHelper Bean
     */
    @Bean
    public CacheHelper cacheHelper(StringRedisTemplate template) {
        return new CacheHelper(template);
    }
}
