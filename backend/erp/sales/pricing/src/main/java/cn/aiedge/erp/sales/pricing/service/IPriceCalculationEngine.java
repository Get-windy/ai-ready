package cn.aiedge.erp.sales.pricing.service;

import cn.aiedge.erp.sales.pricing.dto.*;
import cn.aiedge.erp.sales.pricing.enums.PriceStrategyType;
import cn.aiedge.erp.sales.pricing.exception.PriceCalculationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 价格计算引擎服务接口
 * 负责执行具体的价格计算逻辑和策略匹配
 */
public interface IPriceCalculationEngine {
    
    /**
     * 执行价格计算
     */
    PriceCalculationResult calculate(PriceCalculationRequest request) throws PriceCalculationException;
    
    /**
     * 批量价格计算
     */
    List<PriceCalculationResult> calculateBatch(List<PriceCalculationRequest> requests);
    
    /**
     * 标准定价策略计算
     */
    BigDecimal calculateStandardPrice(PriceCalculationRequest request);
    
    /**
     * 客户分级定价策略计算
     */
    BigDecimal calculateCustomerTieredPrice(PriceCalculationRequest request, PriceStrategyDTO strategy);
    
    /**
     * 批量折扣定价策略计算
     */
    BigDecimal calculateBulkDiscountPrice(PriceCalculationRequest request, PriceStrategyDTO strategy);
    
    /**
     * 促销活动定价策略计算
     */
    BigDecimal calculatePromotionPrice(PriceCalculationRequest request, PriceStrategyDTO strategy);
    
    /**
     * 时段定价策略计算
     */
    BigDecimal calculateTimeBasedPrice(PriceCalculationRequest request, PriceStrategyDTO strategy);
    
    /**
     * 组合产品定价计算
     */
    BigDecimal calculateBundlePrice(PriceCalculationRequest request, PriceStrategyDTO strategy);
    
    /**
     * 运费价格计算
     */
    BigDecimal calculateShippingPrice(PriceCalculationRequest request);
    
    /**
     * 税费计算
     */
    BigDecimal calculateTaxPrice(PriceCalculationRequest request, BigDecimal basePrice);
    
    /**
     * 获取适用的定价策略
     */
    List<PriceStrategyDTO> getApplicableStrategies(PriceCalculationRequest request);
    
    /**
     * 策略优先级排序
     */
    List<PriceStrategyDTO> prioritizeStrategies(List<PriceStrategyDTO> strategies);
    
    /**
     * 检测策略冲突
     */
    List<StrategyConflict> detectStrategyConflicts(List<PriceStrategyDTO> strategies);
    
    /**
     * 价格验证和合理性检查
     */
    PriceValidationResult validatePrice(PriceCalculationResult result);
    
    /**
     * 获取价格计算统计信息
     */
    PriceCalculationStats getStatistics();
    
    /**
     * 清理价格缓存
     */
    void clearCache();
    
    /**
     * 预计算价格（性能优化）
     */
    PricePrecalculationResult precalculate(PrecalculationRequest request);
    
    /**
     * 策略冲突信息
     */
    class StrategyConflict {
        private PriceStrategyDTO strategy1;
        private PriceStrategyDTO strategy2;
        private String conflictType;
        private String description;
        
        // getters and setters
    }
    
    /**
     * 价格验证结果
     */
    class PriceValidationResult {
        private boolean valid;
        private List<String> validationErrors;
        private Map<String, Object> validationData;
        
        // getters and setters
    }
    
    /**
     * 价格预计算请求
     */
    class PrecalculationRequest {
        private List<String> productCodes;
        private List<String> customerLevels;
        private List<String> regions;
        private String timeRange;
        
        // getters and setters
    }
    
    /**
     * 价格预计算结果
     */
    class PricePrecalculationResult {
        private Map<String, Map<String, BigDecimal>> precalculatedPrices;
        private Map<String, List<PriceStrategyDTO>> applicableStrategies;
        private long calculationTimeMs;
        
        // getters and setters
    }
}