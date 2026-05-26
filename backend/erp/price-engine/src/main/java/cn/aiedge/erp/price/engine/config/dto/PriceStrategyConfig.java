package cn.aiedge.erp.price.engine.config.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 价格策略配置DTO
 * 使用record表示DTO，符合项目规范
 */
public record PriceStrategyConfig(
        String strategyId,
        String strategyName,
        StrategyType strategyType,
        String description,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        List<PriceCondition> conditions,
        List<PriceRule> rules,
        Integer priority,
        ConflictResolution conflictResolution,
        ValidationStatus validationStatus
) {
    public enum StrategyType {
        FIXED_PRICE,     // 固定价格
        DISCOUNT,        // 折扣
        TIERED_PRICING,  // 阶梯定价
        DYNAMIC_PRICING, // 动态定价
        PROMOTIONAL,     // 促销价格
        CUSTOM          // 自定义
    }
    
    public enum ConflictResolution {
        HIGHER_PRIORITY_WINS,    // 高优先级胜出
        FIRST_IN_WINS,           // 先到先得
        COMBINE,                // 合并
        REJECT                  // 拒绝
    }
    
    public enum ValidationStatus {
        DRAFT,          // 草稿
        VALIDATED,      // 已验证
        INVALID,        // 无效
        EXPIRED         // 已过期
    }
}