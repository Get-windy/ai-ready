package cn.aiedge.recommendation.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

/**
 * 智能推荐模块配置
 * 配置推荐模块所需的 Bean 和参数
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Configuration
public class RecommendationConfig {

    /**
     * 配置 Redis Template 用于推荐缓存
     *
     * @param connectionFactory Redis 连接工厂
     * @return RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // 设置序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 推荐算法配置参数
     */
    public static class RecommendationAlgorithmConfig {
        // 协同过滤最小相似度阈值
        public static final double MIN_SIMILARITY_THRESHOLD = 0.1;
        
        // 个性化推荐权重
        public static final double PERSONALIZATION_WEIGHT = 0.7;
        
        // 热门推荐权重
        public static final double HOT_ITEM_WEIGHT = 0.3;
        
        // 用户行为分数权重
        public static final Map<String, Double> BEHAVIOR_TYPE_WEIGHTS = Map.of(
            "VIEW", 1.0,
            "CLICK", 2.0,
            "FAVORITE", 3.0,
            "CART", 4.0,
            "PURCHASE", 5.0
        );
        
        // 推荐缓存过期时间（秒）
        public static final int RECOMMENDATION_CACHE_EXPIRE_TIME = 3600;
        
        // 行为历史缓存过期时间（秒）
        public static final int BEHAVIOR_HISTORY_CACHE_EXPIRE_TIME = 86400;
    }
}
