package cn.aiedge.erp.sales.pricing.service.impl;

import cn.aiedge.erp.sales.pricing.dto.PriceStrategyDTO;
import cn.aiedge.erp.sales.pricing.entity.PriceStrategy;
import cn.aiedge.erp.sales.pricing.enums.CustomerLevel;
import cn.aiedge.erp.sales.pricing.enums.PriceStrategyType;
import cn.aiedge.erp.sales.pricing.repository.PriceStrategyRepository;
import cn.aiedge.erp.sales.pricing.service.IPriceStrategyService;
import cn.aiedge.erp.sales.pricing.service.algorithm.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 增强版价格策略服务实现类
 * 集成标准定价算法、客户分级定价算法和促销定价算法
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EnhancedPriceStrategyServiceImpl implements IPriceStrategyService {
    
    private final PriceStrategyRepository priceStrategyRepository;
    private final IStandardPricingAlgorithm standardPricingAlgorithm;
    private final ICustomerTieredPricingAlgorithm customerTieredPricingAlgorithm;
    private final IPromotionPricingAlgorithm promotionPricingAlgorithm;
    
    @Override
    public PriceStrategy createStrategy(PriceStrategyDTO strategyDTO) {
        log.info("创建价格策略: {}", strategyDTO.getName());
        
        PriceStrategy strategy = new PriceStrategy();
        BeanUtils.copyProperties(strategyDTO, strategy, "id", "createTime", "updateTime");
        
        strategy.setCreateTime(LocalDateTime.now());
        strategy.setUpdateTime(LocalDateTime.now());
        
        return priceStrategyRepository.save(strategy);
    }
    
    @Override
    public PriceStrategy updateStrategy(Long id, PriceStrategyDTO strategyDTO) {
        log.info("更新价格策略: id={}", id);
        
        PriceStrategy strategy = priceStrategyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("价格策略不存在: " + id));
        
        BeanUtils.copyProperties(strategyDTO, strategy, "id", "createTime", "updateTime");
        strategy.setUpdateTime(LocalDateTime.now());
        
        return priceStrategyRepository.save(strategy);
    }
    
    @Override
    public void deleteStrategy(Long id) {
        log.info("删除价格策略: id={}", id);
        
        PriceStrategy strategy = priceStrategyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("价格策略不存在: " + id));
        
        strategy.setDeleted(1);
        strategy.setUpdateTime(LocalDateTime.now());
        
        priceStrategyRepository.save(strategy);
    }
    
    @Override
    public PriceStrategy getStrategy(Long id) {
        return priceStrategyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("价格策略不存在: " + id));
    }
    
    @Override
    public Page<PriceStrategy> listStrategies(Pageable pageable) {
        return priceStrategyRepository.findAll(pageable);
    }
    
    @Override
    public List<PriceStrategy> listStrategiesByTenant(Long tenantId) {
        return priceStrategyRepository.findByTenantIdAndStatus(tenantId, "active");
    }
    
    @Override
    public List<PriceStrategy> listStrategiesByCustomerLevel(String customerLevel) {
        return priceStrategyRepository.findByCustomerLevelAndStatus(customerLevel, "active");
    }
    
    @Override
    public List<PriceStrategy> listStrategiesByProductCategory(Long productCategoryId) {
        return priceStrategyRepository.findByProductCategoryIdAndStatus(productCategoryId, "active");
    }
    
    @Override
    public PriceStrategy activateStrategy(Long id) {
        log.info("激活价格策略: id={}", id);
        
        PriceStrategy strategy = getStrategy(id);
        strategy.setStatus("active");
        strategy.setUpdateTime(LocalDateTime.now());
        
        return priceStrategyRepository.save(strategy);
    }
    
    @Override
    public PriceStrategy deactivateStrategy(Long id) {
        log.info("停用价格策略: id={}", id);
        
        PriceStrategy strategy = getStrategy(id);
        strategy.setStatus("inactive");
        strategy.setUpdateTime(LocalDateTime.now());
        
        return priceStrategyRepository.save(strategy);
    }
    
    @Override
    public PriceStrategy copyStrategy(Long sourceId, String newStrategyName) {
        log.info("复制价格策略: sourceId={}, newName={}", sourceId, newStrategyName);
        
        PriceStrategy sourceStrategy = getStrategy(sourceId);
        PriceStrategy newStrategy = new PriceStrategy();
        
        BeanUtils.copyProperties(sourceStrategy, newStrategy, "id", "name", "createTime", "updateTime");
        newStrategy.setName(newStrategyName);
        newStrategy.setCreateTime(LocalDateTime.now());
        newStrategy.setUpdateTime(LocalDateTime.now());
        
        return priceStrategyRepository.save(newStrategy);
    }
    
    @Override
    public List<PriceStrategy> importStrategies(List<PriceStrategyDTO> strategyDTOs) {
        log.info("导入价格策略: count={}", strategyDTOs.size());
        
        return strategyDTOs.stream()
                .map(this::createStrategy)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<PriceStrategyDTO> exportStrategies(List<Long> strategyIds) {
        log.info("导出价格策略: ids={}", strategyIds);
        
        return strategyIds.stream()
                .map(this::getStrategy)
                .map(strategy -> {
                    PriceStrategyDTO dto = new PriceStrategyDTO();
                    BeanUtils.copyProperties(strategy, dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    // ========== 增强方法 ==========
    
    /**
     * 计算标准价格
     * 
     * @param basePrice 基础价格
     * @param strategyType 策略类型
     * @param cost 成本（可选）
     * @param marketPrice 市场参考价（可选）
     * @param competitorPrice 竞争对手价格（可选）
     * @return 计算后的价格
     */
    public BigDecimal calculateStandardPrice(BigDecimal basePrice, PriceStrategyType strategyType,
                                            BigDecimal cost, BigDecimal marketPrice, 
                                            BigDecimal competitorPrice) {
        log.info("计算标准价格: basePrice={}, strategyType={}", basePrice, strategyType);
        
        switch (strategyType) {
            case STANDARD_COST_PLUS:
                if (cost == null) {
                    throw new IllegalArgumentException("成本加成定价需要成本参数");
                }
                return standardPricingAlgorithm.calculateCostPlusPrice(cost, BigDecimal.valueOf(0.20));
                
            case STANDARD_TARGET_PROFIT:
                if (cost == null) {
                    throw new IllegalArgumentException("目标利润定价需要成本参数");
                }
                return standardPricingAlgorithm.calculateTargetProfitPrice(cost, BigDecimal.valueOf(0.15));
                
            case STANDARD_MARKET_REFERENCE:
                if (marketPrice == null) {
                    throw new IllegalArgumentException("市场参考定价需要市场参考价参数");
                }
                return standardPricingAlgorithm.calculateMarketReferencePrice(marketPrice, BigDecimal.ONE);
                
            case STANDARD_COMPETITION_BASED:
                if (competitorPrice == null) {
                    throw new IllegalArgumentException("竞争导向定价需要竞争对手价格参数");
                }
                return standardPricingAlgorithm.calculateCompetitionBasedPrice(competitorPrice, BigDecimal.valueOf(0.95));
                
            default:
                throw new IllegalArgumentException("不支持的标准定价策略类型: " + strategyType);
        }
    }
    
    /**
     * 计算客户分级价格
     * 
     * @param basePrice 基础价格
     * @param customerLevel 客户等级
     * @param historicalPurchaseAmount 历史采购金额
     * @param purchaseCount 采购次数
     * @param loyaltyScore 忠诚度分数
     * @param customerAgeMonths 客户时长（月数）
     * @return 计算后的价格
     */
    public BigDecimal calculateCustomerTieredPrice(BigDecimal basePrice, CustomerLevel customerLevel,
                                                  BigDecimal historicalPurchaseAmount, int purchaseCount,
                                                  int loyaltyScore, int customerAgeMonths) {
        log.info("计算客户分级价格: basePrice={}, customerLevel={}", basePrice, customerLevel);
        
        return customerTieredPricingAlgorithm.calculateComprehensiveCustomerPrice(
            basePrice, customerLevel, historicalPurchaseAmount, purchaseCount, 
            loyaltyScore, customerAgeMonths);
    }
    
    /**
     * 验证价格策略
     * 
     * @param price 计算出的价格
     * @param basePrice 基础价格
     * @param strategyType 策略类型
     * @param customerLevel 客户等级（可选）
     * @param minPrice 最低价格（可选）
     * @param maxPrice 最高价格（可选）
     * @return 验证结果
     */
    public PriceValidationResult validatePriceStrategy(BigDecimal price, BigDecimal basePrice,
                                                      PriceStrategyType strategyType,
                                                      CustomerLevel customerLevel,
                                                      BigDecimal minPrice, BigDecimal maxPrice) {
        log.debug("验证价格策略: price={}, basePrice={}, strategyType={}", 
                 price, basePrice, strategyType);
        
        // 标准价格验证
        IStandardPricingAlgorithm.PriceValidationResult standardResult = 
            standardPricingAlgorithm.validatePrice(price, minPrice, maxPrice);
        
        if (!standardResult.isValid()) {
            return new PriceValidationResult(false, 
                "标准价格验证失败: " + standardResult.getMessage(),
                standardResult.getAdjustedPrice());
        }
        
        // 客户分级价格验证（如果提供了客户等级）
        if (customerLevel != null) {
            ICustomerTieredPricingAlgorithm.CustomerPriceValidationResult customerResult =
                customerTieredPricingAlgorithm.validateCustomerPrice(price, customerLevel, basePrice);
            
            if (!customerResult.isValid()) {
                return new PriceValidationResult(false,
                    "客户分级价格验证失败: " + customerResult.getMessage(),
                    customerResult.getAdjustedPrice());
            }
        }
        
        return new PriceValidationResult(true, 
            "价格策略验证通过: " + price, price);
    }
    
    /**
     * 批量计算价格
     * 
     * @param priceRequests 价格请求列表
     * @return 价格计算结果列表
     */
    public List<PriceCalculationResult> batchCalculatePrices(List<PriceCalculationRequest> priceRequests) {
        log.info("批量计算价格: count={}", priceRequests.size());
        
        return priceRequests.stream()
                .map(this::calculateSinglePrice)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取策略优先级
     * 
     * @param strategyTypes 策略类型列表
     * @return 策略优先级排序结果
     */
    public StrategyPriorityResult getStrategyPriority(List<PriceStrategyType> strategyTypes) {
        log.debug("获取策略优先级: types={}", strategyTypes);
        
        List<StrategyPriorityItem> priorityItems = strategyTypes.stream()
                .map(type -> new StrategyPriorityItem(type, getPriorityWeight(type)))
                .sorted((a, b) -> b.getWeight().compareTo(a.getWeight()))
                .collect(Collectors.toList());
        
        return new StrategyPriorityResult(priorityItems, "权重降序排序");
    }
    
    // ========== 私有方法 ==========
    
    /**
     * 计算单个价格
     */
    private PriceCalculationResult calculateSinglePrice(PriceCalculationRequest request) {
        try {
            BigDecimal price = request.getBasePrice();
            
            // 应用标准定价策略
            if (request.getStandardStrategyType() != null) {
                price = calculateStandardPrice(price, request.getStandardStrategyType(),
                                             request.getCost(), request.getMarketPrice(), 
                                             request.getCompetitorPrice());
            }
            
            // 应用客户分级定价策略
            if (request.getCustomerLevel() != null) {
                price = calculateCustomerTieredPrice(price, request.getCustomerLevel(),
                                                   request.getHistoricalPurchaseAmount(),
                                                   request.getPurchaseCount(),
                                                   request.getLoyaltyScore(),
                                                   request.getCustomerAgeMonths());
            }
            
            // 验证价格
            PriceValidationResult validationResult = validatePriceStrategy(
                price, request.getBasePrice(), request.getStandardStrategyType(),
                request.getCustomerLevel(), request.getMinPrice(), request.getMaxPrice());
            
            return new PriceCalculationResult(request.getRequestId(), price,
                                            validationResult.isValid() ? "SUCCESS" : "VALIDATION_ERROR",
                                            validationResult.getMessage(), 
                                            validationResult.getAdjustedPrice());
            
        } catch (Exception e) {
            log.error("计算价格失败: requestId={}, error={}", request.getRequestId(), e.getMessage(), e);
            return new PriceCalculationResult(request.getRequestId(), null,
                                            "ERROR", e.getMessage(), null);
        }
    }
    
    /**
     * 获取策略权重
     */
    private Integer getPriorityWeight(PriceStrategyType strategyType) {
        switch (strategyType) {
            case CUSTOMER_TIERED:
                return 100; // 客户分级策略优先级最高
            case PROMOTION:
                return 90;  // 促销策略
            case STANDARD_COST_PLUS:
            case STANDARD_TARGET_PROFIT:
                return 80;  // 标准成本策略
            case STANDARD_MARKET_REFERENCE:
            case STANDARD_COMPETITION_BASED:
                return 70;  // 市场参考策略
            case VOLUME_DISCOUNT:
                return 60;  // 批量折扣策略
            default:
                return 50;  // 其他策略
        }
    }
    
    // ========== 内部类 ==========
    
    /**
     * 价格验证结果
     */
    public static class PriceValidationResult {
        private final boolean valid;
        private final String message;
        private final BigDecimal adjustedPrice;
        
        public PriceValidationResult(boolean valid, String message, BigDecimal adjustedPrice) {
            this.valid = valid;
            this.message = message;
            this.adjustedPrice = adjustedPrice;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public BigDecimal getAdjustedPrice() { return adjustedPrice; }
    }
    
    /**
     * 价格计算请求
     */
    public static class PriceCalculationRequest {
        private final String requestId;
        private final BigDecimal basePrice;
        private final PriceStrategyType standardStrategyType;
        private final CustomerLevel customerLevel;
        private final BigDecimal cost;
        private final BigDecimal marketPrice;
        private final BigDecimal competitorPrice;
        private final BigDecimal historicalPurchaseAmount;
        private final int purchaseCount;
        private final int loyaltyScore;
        private final int customerAgeMonths;
        private final BigDecimal minPrice;
        private final BigDecimal maxPrice;
        
        public PriceCalculationRequest(String requestId, BigDecimal basePrice,
                                      PriceStrategyType standardStrategyType, CustomerLevel customerLevel,
                                      BigDecimal cost, BigDecimal marketPrice, BigDecimal competitorPrice,
                                      BigDecimal historicalPurchaseAmount, int purchaseCount,
                                      int loyaltyScore, int customerAgeMonths,
                                      BigDecimal minPrice, BigDecimal maxPrice) {
            this.requestId = requestId;
            this.basePrice = basePrice;
            this.standardStrategyType = standardStrategyType;
            this.customerLevel = customerLevel;
            this.cost = cost;
            this.marketPrice = marketPrice;
            this.competitorPrice = competitorPrice;
            this.historicalPurchaseAmount = historicalPurchaseAmount;
            this.purchaseCount = purchaseCount;
            this.loyaltyScore = loyaltyScore;
            this.customerAgeMonths = customerAgeMonths;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
        }
        
        // Getter方法
        public String getRequestId() { return requestId; }
        public BigDecimal getBasePrice() { return basePrice; }
        public PriceStrategyType getStandardStrategyType() { return standardStrategyType; }
        public CustomerLevel getCustomerLevel() { return customerLevel; }
        public BigDecimal getCost() { return cost; }
        public BigDecimal getMarketPrice() { return marketPrice; }
        public BigDecimal getCompetitorPrice() { return competitorPrice; }
        public BigDecimal getHistoricalPurchaseAmount() { return historicalPurchaseAmount; }
        public int getPurchaseCount() { return purchaseCount; }
        public int getLoyaltyScore() { return loyaltyScore; }
        public int getCustomerAgeMonths() { return customerAgeMonths; }
        public BigDecimal getMinPrice() { return minPrice; }
        public BigDecimal getMaxPrice() { return maxPrice; }
    }
    
    /**
     * 价格计算结果
     */
    public static class PriceCalculationResult {
        private final String requestId;
        private final BigDecimal calculatedPrice;
        private final String status;
        private final String message;
        private final BigDecimal adjustedPrice;
        
        public PriceCalculationResult(String requestId, BigDecimal calculatedPrice,
                                     String status, String message, BigDecimal adjustedPrice) {
            this.requestId = requestId;
            this.calculatedPrice = calculatedPrice;
            this.status = status;
            this.message = message;
            this.adjustedPrice = adjustedPrice;
        }
        
        public String getRequestId() { return requestId; }
        public BigDecimal getCalculatedPrice() { return calculatedPrice; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public BigDecimal getAdjustedPrice() { return adjustedPrice; }
    }
    
    /**
     * 策略优先级项
     */
    public static class StrategyPriorityItem {
        private final PriceStrategyType strategyType;
        private final Integer weight;
        
        public StrategyPriorityItem(PriceStrategyType strategyType, Integer weight) {
            this.strategyType = strategyType;
            this.weight = weight;
        }
        
        public PriceStrategyType getStrategyType() { return strategyType; }
        public Integer getWeight() { return weight; }
    }
    
    /**
     * 策略优先级结果
     */
    public static class StrategyPriorityResult {
        private final List<StrategyPriorityItem> priorityItems;
        private final String sortingMethod;
        
        public StrategyPriorityResult(List<StrategyPriorityItem> priorityItems, String sortingMethod) {
            this.priorityItems = priorityItems;
            this.sortingMethod = sortingMethod;
        }
        
        public List<StrategyPriorityItem> getPriorityItems() { return priorityItems; }
        public String getSortingMethod() { return sortingMethod; }
    }
}