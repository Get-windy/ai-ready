package cn.aiedge.erp.sales.pricing.service;

import cn.aiedge.erp.sales.pricing.dto.PriceCalculationRequest;
import cn.aiedge.erp.sales.pricing.dto.PriceCalculationResult;

/**
 * 价格计算服务接口
 */
public interface IPriceCalculationService {
    
    /**
     * 计算商品价格
     */
    PriceCalculationResult calculatePrice(PriceCalculationRequest request);
    
    /**
     * 批量计算商品价格
     */
    PriceCalculationResult calculateBatchPrice(PriceCalculationRequest request);
    
    /**
     * 验证价格策略是否适用
     */
    boolean validateStrategyApplicability(PriceCalculationRequest request, Long strategyId);
    
    /**
     * 获取价格计算历史
     */
    PriceCalculationResult getCalculationHistory(String calculationId);
    
    /**
     * 模拟价格计算（不保存历史）
     */
    PriceCalculationResult simulatePriceCalculation(PriceCalculationRequest request);
    
    /**
     * 清除价格缓存
     */
    void clearPriceCache();
    
    /**
     * 刷新规则引擎缓存
     */
    void refreshRulesCache();
    
    /**
     * 获取价格计算统计信息
     */
    PriceCalculationStats getCalculationStats();
    
    /**
     * 价格计算统计信息
     */
    class PriceCalculationStats {
        private long totalCalculations;
        private long successfulCalculations;
        private long failedCalculations;
        private double averageCalculationTimeMs;
        
        // getters and setters
    }
}