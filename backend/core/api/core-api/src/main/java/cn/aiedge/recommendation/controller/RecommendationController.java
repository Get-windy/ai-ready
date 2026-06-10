package cn.aiedge.recommendation.controller;

import cn.aiedge.recommendation.entity.UserBehavior;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.aiedge.recommendation.model.RecommendationRequest;
import cn.aiedge.recommendation.model.RecommendationResponse;
import cn.aiedge.recommendation.service.RecommendationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能推荐控制器
 * 提供智能推荐相关的 REST API 接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/recommendation")
@SaCheckLogin
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    /**
     * 获取智能推荐
     *
     * @param request 推荐请求
     * @return 推荐响应
     */
    @PostMapping("/get")
    public RecommendationResponse getRecommendations(@RequestBody RecommendationRequest request) {
        return recommendationService.getRecommendations(request);
    }

    /**
     * 获取个性化推荐
     *
     * @param userId   用户 ID
     * @param limit    数量限制
     * @param tenantId 租户 ID
     * @return 推荐响应
     */
    @GetMapping("/personalized")
    public RecommendationResponse getPersonalizedRecommendations(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam Long tenantId) {
        return recommendationService.getPersonalizedRecommendations(userId, limit, tenantId);
    }

    /**
     * 获取相关推荐
     *
     * @param targetType 目标类型
     * @param targetId   目标 ID
     * @param limit      数量限制
     * @param tenantId   租户 ID
     * @return 推荐响应
     */
    @GetMapping("/related")
    public RecommendationResponse getRelatedRecommendations(
            @RequestParam String targetType,
            @RequestParam Long targetId,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam Long tenantId) {
        return recommendationService.getRelatedRecommendations(targetType, targetId, limit, tenantId);
    }

    /**
     * 获取热门推荐
     *
     * @param targetType 目标类型
     * @param limit      数量限制
     * @param tenantId   租户 ID
     * @param timeRange  时间范围（小时）
     * @return 推荐响应
     */
    @GetMapping("/hot")
    public RecommendationResponse getHotRecommendations(
            @RequestParam String targetType,
            @RequestParam(defaultValue = "10") Integer limit,
            @RequestParam Long tenantId,
            @RequestParam(defaultValue = "24") Integer timeRange) {
        return recommendationService.getHotRecommendations(targetType, limit, tenantId, timeRange);
    }

    /**
     * 记录用户行为
     *
     * @param behavior 用户行为
     */
    @PostMapping("/behavior")
    public void recordBehavior(@RequestBody UserBehavior behavior) {
        recommendationService.recordBehavior(behavior);
    }

    /**
     * 批量记录用户行为
     *
     * @param behaviors 用户行为列表
     */
    @PostMapping("/behaviors")
    public void recordBehaviors(@RequestBody List<UserBehavior> behaviors) {
        recommendationService.recordBehaviors(behaviors);
    }

    /**
     * 获取用户行为历史
     *
     * @param userId   用户 ID
     * @param limit    数量限制
     * @param tenantId 租户 ID
     * @return 用户行为列表
     */
    @GetMapping("/behavior/history")
    public List<UserBehavior> getUserBehaviorHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "20") Integer limit,
            @RequestParam Long tenantId) {
        return recommendationService.getUserBehaviorHistory(userId, limit, tenantId);
    }

    /**
     * 清除用户推荐缓存
     *
     * @param userId   用户 ID
     * @param tenantId 租户 ID
     * @return 是否成功
     */
    @DeleteMapping("/cache")
    public boolean clearUserCache(
            @RequestParam Long userId,
            @RequestParam Long tenantId) {
        recommendationService.clearUserCache(userId, tenantId);
        return true;
    }
}
