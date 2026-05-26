package cn.aiedge.recommendation.service;

import cn.aiedge.recommendation.entity.UserBehavior;
import cn.aiedge.recommendation.model.RecommendationRequest;
import cn.aiedge.recommendation.model.RecommendationResponse;

import java.util.List;

/**
 * 智能推荐服务接口
 * 提供基于用户行为的智能推荐功能
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface RecommendationService {

    /**
     * 获取智能推荐
     *
     * @param request 推荐请求
     * @return 推荐响应
     */
    RecommendationResponse getRecommendations(RecommendationRequest request);

    /**
     * 记录用户行为
     *
     * @param behavior 用户行为
     */
    void recordBehavior(UserBehavior behavior);

    /**
     * 批量记录用户行为
     *
     * @param behaviors 用户行为列表
     */
    void recordBehaviors(List<UserBehavior> behaviors);

    /**
     * 获取用户行为历史
     *
     * @param userId   用户 ID
     * @param limit    数量限制
     * @param tenantId 租户 ID
     * @return 用户行为列表
     */
    List<UserBehavior> getUserBehaviorHistory(Long userId, Integer limit, Long tenantId);

    /**
     * 获取相似目标推荐（基于协同过滤）
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @param limit      数量限制
     * @param tenantId   租户 ID
     * @return 推荐列表
     */
    RecommendationResponse getRelatedRecommendations(String targetType, Long targetId, Integer limit, Long tenantId);

    /**
     * 获取个性化推荐（基于用户历史行为）
     *
     * @param userId   用户 ID
     * @param limit    数量限制
     * @param tenantId 租户 ID
     * @return 推荐列表
     */
    RecommendationResponse getPersonalizedRecommendations(Long userId, Integer limit, Long tenantId);

    /**
     * 获取热门推荐（基于全局热度）
     *
     * @param targetType 目标类型
     * @param limit      数量限制
     * @param tenantId   租户 ID
     * @param timeRange  时间范围（小时）
     * @return 推荐列表
     */
    RecommendationResponse getHotRecommendations(String targetType, Integer limit, Long tenantId, Integer timeRange);

    /**
     * 清除用户推荐缓存
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     */
    void clearUserCache(Long userId, Long tenantId);

    /**
     * 更新推荐模型
     * 定期调用以更新推荐算法模型
     *
     * @param tenantId 租户 ID
     */
    void updateRecommendationModel(Long tenantId);
}
