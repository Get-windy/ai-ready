package cn.aiedge.erp.price.engine.optimization.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 价格优化请求DTO
 */
public record OptimizationRequest(
        String requestId,
        OptimizationType optimizationType,
        OptimizationObjective objective,
        List<PriceVariable> priceVariables,
        List<Constraint> constraints,
        Map<String, Object> context,
        OptimizationParameters parameters,
        String algorithm
) {
    public enum OptimizationType {
        SINGLE_PRODUCT,         // 单产品优化
        MULTI_PRODUCT,          // 多产品优化
        PORTFOLIO,             // 产品组合优化
        PROMOTIONAL            // 促销优化
    }
    
    public enum OptimizationObjective {
        MAXIMIZE_PROFIT,        // 最大化利润
        MAXIMIZE_REVENUE,       // 最大化收入
        MAXIMIZE_MARKET_SHARE,  // 最大化市场份额
        BALANCED,               // 平衡目标
        CUSTOM                  // 自定义目标
    }
}

/**
 * 价格变量
 */
record PriceVariable(
        String productId,
        String variableName,
        BigDecimal currentValue,
        BigDecimal minValue,
        BigDecimal maxValue,
        BigDecimal stepSize,
        VariableType variableType
) {
    enum VariableType {
        BASE_PRICE,             // 基础价格
        DISCOUNT_PERCENTAGE,    // 折扣百分比
        PROMOTIONAL_AMOUNT,     // 促销金额
        TIER_THRESHOLD,         // 阶梯阈值
        CUSTOM                  // 自定义变量
    }
}

/**
 * 优化约束
 */
record Constraint(
        String constraintId,
        ConstraintType constraintType,
        String expression,
        BigDecimal lowerBound,
        BigDecimal upperBound,
        String description,
        ConstraintSeverity severity
) {
    enum ConstraintType {
        PRICE_RANGE,            // 价格范围
        MARGIN_REQUIREMENT,     // 利润率要求
        VOLUME_TARGET,          // 销量目标
        COMPETITIVE_PRICING,    // 竞争定价
        REGULATORY,             // 法规约束
        CUSTOM                  // 自定义约束
    }
    
    enum ConstraintSeverity {
        HARD,                   // 硬约束（必须满足）
        SOFT,                   // 软约束（尽量满足）
        INFORMATIONAL           // 信息性约束
    }
}

/**
 * 优化参数
 */
record OptimizationParameters(
        int maxIterations,
        int populationSize,
        double mutationRate,
        double crossoverRate,
        double convergenceThreshold,
        int randomSeed,
        boolean parallelExecution,
        int timeoutSeconds
) {}