package cn.aiedge.erp.price.engine.config.dto;

import java.math.BigDecimal;

/**
 * 价格规则DTO
 * 表示价格计算规则
 */
public record PriceRule(
        String ruleId,
        String ruleName,
        RuleType ruleType,
        String formula,
        BigDecimal baseValue,
        BigDecimal adjustmentValue,
        AdjustmentType adjustmentType,
        BigDecimal minValue,
        BigDecimal maxValue,
        String currency,
        Integer executionOrder,
        String description
) {
    public enum RuleType {
        FIXED_AMOUNT,           // 固定金额
        PERCENTAGE,             // 百分比
        FORMULA_BASED,          // 公式计算
        LOOKUP_TABLE,           // 查表
        COMPOSITE               // 复合规则
    }
    
    public enum AdjustmentType {
        ADD,                    // 加
        SUBTRACT,               // 减
        MULTIPLY,               // 乘
        DIVIDE,                 // 除
        OVERRIDE                // 覆盖
    }
}