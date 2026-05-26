package cn.aiedge.erp.price.engine.strategy;

import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.price.engine.strategy.entity.PricingStrategy;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationResult;
import java.math.BigDecimal;

/**
 * 定价算法接口
 * 所有定价策略必须实现此接口
 */
public interface PricingAlgorithm {
    
    /**
     * 获取算法类型
     */
    PricingStrategy.StrategyType getAlgorithmType();
    
    /**
     * 检查算法是否适用于给定的计算请求
     */
    boolean isApplicable(PriceCalculationRequest request);
    
    /**
     * 计算价格
     */
    PriceCalculationResult calculatePrice(PriceCalculationRequest request, PricingStrategy strategy);
    
    /**
     * 计算优先级分数 - 用于策略选择，分数越高优先级越高
     */
    int calculatePriorityScore(PriceCalculationRequest request);
    
    /**
     * 获取算法描述
     */
    String getDescription();
    
    /**
     * 获取默认参数配置
     */
    String getDefaultParameters();
}