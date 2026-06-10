package cn.aiedge.recommendation.service.impl;

import cn.aiedge.recommendation.entity.UserBehavior;
import cn.aiedge.recommendation.model.RecommendationRequest;
import cn.aiedge.recommendation.model.RecommendationResponse;
import cn.aiedge.recommendation.service.RecommendationService;
import cn.aiedge.recommendation.mapper.UserBehaviorMapper;
import cn.aiedge.recommendation.mapper.RecommendationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能推荐服务实现
 * 实现基于用户行为的智能推荐算法
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class RecommendationServiceImpl implements RecommendationService {

    @Autowired
    private UserBehaviorMapper userBehaviorMapper;

    @Autowired
    private RecommendationMapper recommendationMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取智能推荐
     *
     * @param request 推荐请求
     * @return 推荐响应
     */
    @Override
    public RecommendationResponse getRecommendations(RecommendationRequest request) {
        switch (request.getType()) {
            case "PERSONALIZED":
                return getPersonalizedRecommendations(request.getUserId(), request.getLimit(), request.getTenantId());
            case "RELATED":
                return getRelatedRecommendations(request.getTargetType(), request.getTargetId(), request.getLimit(), request.getTenantId());
            case "HOT":
                return getHotRecommendations(request.getTargetType(), request.getLimit(), request.getTenantId(), 24);
            default:
                return getPersonalizedRecommendations(request.getUserId(), request.getLimit(), request.getTenantId());
        }
    }

    /**
     * 记录用户行为
     *
     * @param behavior 用户行为
     */
    @Override
    public void recordBehavior(UserBehavior behavior) {
        // 保存到数据库
        userBehaviorMapper.insert(behavior);

        // 更新缓存
        String cacheKey = "user:behavior:" + behavior.getUserId() + ":" + behavior.getTenantId();
        redisTemplate.opsForList().leftPush(cacheKey, behavior);
        redisTemplate.expire(cacheKey, java.time.Duration.ofHours(24));

        // 触发推荐模型更新
        triggerRecommendationUpdate(behavior.getUserId(), behavior.getTenantId());
    }

    /**
     * 批量记录用户行为
     *
     * @param behaviors 用户行为列表
     */
    @Override
    public void recordBehaviors(List<UserBehavior> behaviors) {
        if (behaviors != null && !behaviors.isEmpty()) {
            for (UserBehavior behavior : behaviors) {
                recordBehavior(behavior);
            }
        }
    }

    /**
     * 获取用户行为历史
     *
     * @param userId   用户 ID
     * @param limit    数量限制
     * @param tenantId 租户 ID
     * @return 用户行为列表
     */
    @Override
    public List<UserBehavior> getUserBehaviorHistory(Long userId, Integer limit, Long tenantId) {
        // 尝试从缓存获取
        String cacheKey = "user:behavior:" + userId + ":" + tenantId;
        List<UserBehavior> cached = (List<UserBehavior>) redisTemplate.opsForValue().get(cacheKey);
        
        if (cached != null && !cached.isEmpty()) {
            return cached;
        }

        // 从数据库获取
        List<UserBehavior> behaviors = userBehaviorMapper.selectByUserIdAndTenantId(userId, tenantId, limit);
        
        // 缓存结果
        if (behaviors != null && !behaviors.isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, behaviors);
            redisTemplate.expire(cacheKey, java.time.Duration.ofHours(24));
        }

        return behaviors;
    }

    /**
     * 获取相似目标推荐（基于协同过滤）
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @param limit      数量限制
     * @param tenantId   租户 ID
     * @return 推荐列表
     */
    @Override
    public RecommendationResponse getRelatedRecommendations(String targetType, Long targetId, Integer limit, Long tenantId) {
        // 使用协同过滤算法获取相关推荐
        List<UserBehavior> similarBehaviors = userBehaviorMapper.selectSimilarBehaviors(targetType, targetId, tenantId, limit * 2);

        // 计算相似度并生成推荐
        Map<Long, Double> similarityScores = new HashMap<>();
        for (UserBehavior behavior : similarBehaviors) {
            if (!behavior.getTargetId().equals(targetId)) {
                similarityScores.merge(behavior.getTargetId(), behavior.getBehaviorScore(), Double::sum);
            }
        }

        // 按分数排序并获取 top N
        List<Map.Entry<Long, Double>> sortedEntries = similarityScores.entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());

        List<RecommendationResponse.RecommendationItem> items = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : sortedEntries) {
            // 这里需要根据目标类型查询具体的目标名称
            String targetName = getTargetNameById(targetType, entry.getKey());
            items.add(new RecommendationResponse.RecommendationItem(
                    entry.getKey(),
                    targetName,
                    targetType,
                    entry.getValue(),
                    "基于您对类似内容的兴趣",
                    null
            ));
        }

        return new RecommendationResponse(items, items.size(), "RELATED");
    }

    /**
     * 获取个性化推荐（基于用户历史行为）
     *
     * @param userId   用户 ID
     * @param limit    数量限制
     * @param tenantId 租户 ID
     * @return 推荐列表
     */
    @Override
    public RecommendationResponse getPersonalizedRecommendations(Long userId, Integer limit, Long tenantId) {
        // 获取用户行为历史
        List<UserBehavior> userBehaviors = getUserBehaviorHistory(userId, 50, tenantId);

        // 计算用户偏好
        Map<String, Double> typePreferences = calculateTypePreferences(userBehaviors);
        Map<Long, Double> itemPreferences = calculateItemPreferences(userBehaviors);

        // 获取热门项目
        List<UserBehavior> hotItems = userBehaviorMapper.selectHotItems(tenantId, limit * 2);

        // 结合用户偏好和热门项目生成推荐
        Map<Long, Double> combinedScores = new HashMap<>();

        // 加权用户偏好项目
        for (Map.Entry<Long, Double> entry : itemPreferences.entrySet()) {
            combinedScores.merge(entry.getKey(), entry.getValue() * 0.7, Double::sum);
        }

        // 加权热门项目
        for (UserBehavior behavior : hotItems) {
            Double weight = typePreferences.getOrDefault(behavior.getTargetType(), 0.1);
            combinedScores.merge(behavior.getTargetId(), behavior.getBehaviorScore() * 0.3 * weight, Double::sum);
        }

        // 排序并选择 top N
        List<Map.Entry<Long, Double>> sortedEntries = combinedScores.entrySet()
                .stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());

        List<RecommendationResponse.RecommendationItem> items = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : sortedEntries) {
            String targetType = getTargetTypeById(entry.getKey()); // 需要改进此方法
            String targetName = getTargetNameById(targetType, entry.getKey());
            
            items.add(new RecommendationResponse.RecommendationItem(
                    entry.getKey(),
                    targetName,
                    targetType,
                    entry.getValue(),
                    "基于您的历史行为推荐",
                    null
            ));
        }

        return new RecommendationResponse(items, items.size(), "PERSONALIZED");
    }

    /**
     * 获取热门推荐（基于全局热度）
     *
     * @param targetType 目标类型
     * @param limit      数量限制
     * @param tenantId   租户 ID
     * @param timeRange  时间范围（小时）
     * @return 推荐列表
     */
    @Override
    public RecommendationResponse getHotRecommendations(String targetType, Integer limit, Long tenantId, Integer timeRange) {
        List<UserBehavior> hotItems = userBehaviorMapper.selectHotItemsByType(targetType, tenantId, limit, timeRange);

        List<RecommendationResponse.RecommendationItem> items = hotItems.stream()
                .map(behavior -> new RecommendationResponse.RecommendationItem(
                        behavior.getTargetId(),
                        behavior.getTargetName(),
                        behavior.getTargetType(),
                        behavior.getBehaviorScore(),
                        "当前热门推荐",
                        null
                ))
                .collect(Collectors.toList());

        return new RecommendationResponse(items, items.size(), "HOT");
    }

    /**
     * 清除用户推荐缓存
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     */
    @Override
    public void clearUserCache(Long userId, Long tenantId) {
        String cacheKey = "user:behavior:" + userId + ":" + tenantId;
        redisTemplate.delete(cacheKey);
        
        String recommendationCacheKey = "user:recommendation:" + userId + ":" + tenantId;
        redisTemplate.delete(recommendationCacheKey);
    }

    /**
     * 更新推荐模型
     * 定期调用以更新推荐算法模型
     *
     * @param tenantId 租户 ID
     */
    @Override
    public void updateRecommendationModel(Long tenantId) {
        // 实际项目中这里会使用机器学习算法来训练推荐模型
        // 例如使用协同过滤、矩阵分解或深度学习算法
        log.info("Updating recommendation model for tenant: " + tenantId);
        
        // 可以考虑使用定时任务定期调用此方法
        // 或者基于数据变化阈值触发模型更新
    }

    /**
     * 根据 ID 获取目标名称
     */
    private String getTargetNameById(String targetType, Long targetId) {
        // 在实际实现中，需要根据目标类型查询对应的表获取名称
        // 这里只是一个示例实现
        return targetType + "_" + targetId;
    }

    /**
     * 根据 ID 获取目标类型
     */
    private String getTargetTypeById(Long targetId) {
        // 在实际实现中，需要根据 ID 推断目标类型
        // 这里返回默认值
        return "PRODUCT";
    }

    /**
     * 计算用户类型偏好
     */
    private Map<String, Double> calculateTypePreferences(List<UserBehavior> behaviors) {
        Map<String, Double> typeScores = new HashMap<>();
        for (UserBehavior behavior : behaviors) {
            typeScores.merge(behavior.getTargetType(), 
                           behavior.getBehaviorScore(), 
                           Double::sum);
        }
        return typeScores;
    }

    /**
     * 计算用户项目偏好
     */
    private Map<Long, Double> calculateItemPreferences(List<UserBehavior> behaviors) {
        Map<Long, Double> itemScores = new HashMap<>();
        for (UserBehavior behavior : behaviors) {
            itemScores.merge(behavior.getTargetId(), 
                           behavior.getBehaviorScore(), 
                           Double::sum);
        }
        return itemScores;
    }

    /**
     * 触发推荐更新
     */
    private void triggerRecommendationUpdate(Long userId, Long tenantId) {
        // 在实际实现中，可能会使用异步任务来更新推荐
        // 这里简单地清除缓存以触发重新计算
        clearUserCache(userId, tenantId);
    }
}
