package cn.aiedge.erp.price.engine.strategy;

import cn.aiedge.erp.price.engine.strategy.entity.DiscountRule;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import java.math.BigDecimal;
import java.util.List;

/**
 * 折扣算法接口
 * 所有折扣策略必须实现此接口
 */
public interface DiscountStrategy {
    
    /**
     * 获取折扣类型
     */
    DiscountRule.DiscountType getDiscountType();
    
    /**
     * 检查折扣规则是否适用于给定的计算请求
     */
    boolean isApplicable(DiscountRule rule, PriceCalculationRequest request);
    
    /**
     * 计算折扣金额
     */
    BigDecimal calculateDiscount(DiscountRule rule, PriceCalculationRequest request, BigDecimal basePrice);
    
    /**
     * 批量计算折扣 - 用于批量订单或组合折扣
     */
    List<BigDecimal> calculateBulkDiscount(DiscountRule rule, List<PriceCalculationRequest> requests, List<BigDecimal> basePrices);
    
    /**
     * 计算折扣优先级分数 - 用于折扣规则排序，分数越高优先级越高
     */
    int calculatePriorityScore(DiscountRule rule, PriceCalculationRequest request);
    
    /**
     * 获取折扣描述
     */
    String getDescription(DiscountRule rule);
    
    /**
     * 验证折扣规则参数
     */
    boolean validateRuleParameters(DiscountRule rule);
}