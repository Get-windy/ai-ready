package cn.aiedge.erp.price.engine.config;

import cn.aiedge.erp.price.engine.config.dto.PriceCondition;
import cn.aiedge.erp.price.engine.config.dto.PriceRule;

import java.util.List;
import java.util.Map;

/**
 * 价格规则引擎接口
 */
public interface IPriceRuleEngine {
    
    /**
     * 加载规则
     * @param rules 价格规则列表
     */
    void loadRules(List<PriceRule> rules);
    
    /**
     * 加载条件
     * @param conditions 价格条件列表
     */
    void loadConditions(List<PriceCondition> conditions);
    
    /**
     * 评估条件
     * @param context 评估上下文
     * @return 匹配的条件列表
     */
    List<PriceCondition> evaluateConditions(Map<String, Object> context);
    
    /**
     * 应用规则
     * @param basePrice 基础价格
     * @param context 应用上下文
     * @return 应用规则后的价格
     */
    double applyRules(double basePrice, Map<String, Object> context);
    
    /**
     * 获取匹配的规则
     * @param context 上下文
     * @return 匹配的规则列表
     */
    List<PriceRule> getMatchingRules(Map<String, Object> context);
    
    /**
     * 验证规则冲突
     * @param rules 规则列表
     * @return 冲突检测结果
     */
    ConflictDetectionResult detectConflicts(List<PriceRule> rules);
    
    /**
     * 解决规则冲突
     * @param conflictingRules 冲突规则列表
     * @param resolutionStrategy 解决策略
     * @return 解决冲突后的规则列表
     */
    List<PriceRule> resolveConflicts(List<PriceRule> conflictingRules, String resolutionStrategy);
    
    /**
     * 规则执行统计
     * @return 统计信息
     */
    RuleExecutionStats getExecutionStats();
    
    /**
     * 清理规则
     */
    void clearRules();
    
    /**
     * 重新加载规则
     */
    void reload();
    
    /**
     * 冲突检测结果
     */
    record ConflictDetectionResult(
            boolean hasConflicts,
            List<Conflict> conflicts,
            String recommendation
    ) {}
    
    /**
     * 冲突信息
     */
    record Conflict(
            PriceRule rule1,
            PriceRule rule2,
            String conflictType,
            String description
    ) {}
    
    /**
     * 规则执行统计
     */
    record RuleExecutionStats(
            long totalExecutions,
            long successfulExecutions,
            long failedExecutions,
            double averageExecutionTimeMs,
            Map<String, Long> ruleExecutionCount
    ) {}
}