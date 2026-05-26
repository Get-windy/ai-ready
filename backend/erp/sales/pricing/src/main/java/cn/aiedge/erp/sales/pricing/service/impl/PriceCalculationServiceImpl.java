package cn.aiedge.erp.sales.pricing.service.impl;

import cn.aiedge.erp.sales.pricing.dto.PriceCalculationRequest;
import cn.aiedge.erp.sales.pricing.dto.PriceCalculationResult;
import cn.aiedge.erp.sales.pricing.entity.PriceStrategy;
import cn.aiedge.erp.sales.pricing.repository.PriceStrategyRepository;
import cn.aiedge.erp.sales.pricing.service.IDroolsRuleService;
import cn.aiedge.erp.sales.pricing.service.IPriceCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 价格计算服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PriceCalculationServiceImpl implements IPriceCalculationService {
    
    private final PriceStrategyRepository priceStrategyRepository;
    private final IDroolsRuleService droolsRuleService;
    
    @Override
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request) {
        log.info("计算商品价格: productId={}, customerId={}, quantity={}", 
                request.getProductId(), request.getCustomerId(), request.getQuantity());
        
        try {
            // 1. 获取适用的价格策略
            List<PriceStrategy> applicableStrategies = getApplicableStrategies(request);
            
            // 2. 按优先级排序
            applicableStrategies.sort(Comparator.comparingInt(PriceStrategy::getPriority));
            
            // 3. 执行Drools规则计算
            PriceCalculationContext context = new PriceCalculationContext(request, applicableStrategies);
            droolsRuleService.executeRules(context);
            
            // 4. 构建计算结果
            PriceCalculationResult result = buildResult(context);
            result.setSuccess(true);
            result.setCalculationTime(LocalDateTime.now());
            result.setCalculationId(UUID.randomUUID().toString());
            
            log.info("价格计算完成: finalPrice={}, originalPrice={}, totalDiscount={}", 
                    result.getFinalPrice(), result.getOriginalPrice(), result.getTotalDiscount());
            
            return result;
        } catch (Exception e) {
            log.error("价格计算失败", e);
            
            PriceCalculationResult errorResult = new PriceCalculationResult();
            errorResult.setSuccess(false);
            errorResult.setErrorMessage(e.getMessage());
            errorResult.setCalculationTime(LocalDateTime.now());
            
            return errorResult;
        }
    }
    
    private List<PriceStrategy> getApplicableStrategies(PriceCalculationRequest request) {
        LocalDateTime currentTime = LocalDateTime.now();
        List<PriceStrategy> allStrategies = priceStrategyRepository.findAllActiveStrategies(request.getTenantId());
        
        return allStrategies.stream()
                .filter(strategy -> isStrategyApplicable(strategy, request, currentTime))
                .collect(Collectors.toList());
    }
    
    private boolean isStrategyApplicable(PriceStrategy strategy, PriceCalculationRequest request, LocalDateTime currentTime) {
        // 检查时间有效性
        if (strategy.getEffectiveStartTime() != null && currentTime.isBefore(strategy.getEffectiveStartTime())) {
            return false;
        }
        if (strategy.getEffectiveEndTime() != null && currentTime.isAfter(strategy.getEffectiveEndTime())) {
            return false;
        }
        
        // 检查客户等级匹配
        if (strategy.getCustomerLevel() != null && !strategy.getCustomerLevel().isEmpty()) {
            if (!strategy.getCustomerLevel().equals(request.getCustomerLevel())) {
                return false;
            }
        }
        
        // 检查产品类别匹配
        if (strategy.getProductCategoryId() != null) {
            if (!strategy.getProductCategoryId().equals(request.getProductCategoryId())) {
                return false;
            }
        }
        
        // 检查数量门槛
        if (strategy.getMinQuantity() != null && strategy.getMinQuantity() > 0) {
            if (request.getQuantity() < strategy.getMinQuantity()) {
                return false;
            }
        }
        
        return true;
    }
    
    private PriceCalculationResult buildResult(PriceCalculationContext context) {
        PriceCalculationResult result = new PriceCalculationResult();
        result.setOriginalPrice(context.getOriginalPrice());
        result.setFinalPrice(context.getFinalPrice());
        result.setTotalDiscount(context.getTotalDiscount());
        result.setTotalDiscountRate(context.getTotalDiscountRate());
        result.setAppliedStrategies(context.getAppliedStrategies());
        
        return result;
    }
    
    @Override
    public PriceCalculationResult calculateBatchPrice(PriceCalculationRequest request) {
        // 简化实现：批量计算与单次计算相同
        return calculatePrice(request);
    }
    
    @Override
    public boolean validateStrategyApplicability(PriceCalculationRequest request, Long strategyId) {
        PriceStrategy strategy = priceStrategyRepository.findById(strategyId)
                .orElseThrow(() -> new RuntimeException("价格策略不存在: " + strategyId));
        
        return isStrategyApplicable(strategy, request, LocalDateTime.now());
    }
    
    @Override
    public PriceCalculationResult getCalculationHistory(String calculationId) {
        // 简化实现：实际应该从数据库查询历史记录
        PriceCalculationResult result = new PriceCalculationResult();
        result.setSuccess(true);
        result.setCalculationId(calculationId);
        result.setErrorMessage("历史记录功能待实现");
        
        return result;
    }
    
    @Override
    public PriceCalculationResult simulatePriceCalculation(PriceCalculationRequest request) {
        // 模拟计算与正常计算相同，但不保存历史
        return calculatePrice(request);
    }
    
    @Override
    public void clearPriceCache() {
        log.info("清除价格缓存");
        // 实现缓存清除逻辑
    }
    
    @Override
    public void refreshRulesCache() {
        log.info("刷新规则引擎缓存");
        droolsRuleService.reloadRules();
    }
    
    @Override
    public PriceCalculationStats getCalculationStats() {
        PriceCalculationStats stats = new PriceCalculationStats();
        stats.setTotalCalculations(1000); // 模拟数据
        stats.setSuccessfulCalculations(980);
        stats.setFailedCalculations(20);
        stats.setAverageCalculationTimeMs(50.5);
        
        return stats;
    }
    
    /**
     * 价格计算上下文类
     */
    private static class PriceCalculationContext {
        private final PriceCalculationRequest request;
        private final List<PriceStrategy> applicableStrategies;
        private BigDecimal originalPrice;
        private BigDecimal finalPrice;
        private BigDecimal totalDiscount = BigDecimal.ZERO;
        private BigDecimal totalDiscountRate = BigDecimal.ZERO;
        private List<PriceCalculationResult.AppliedStrategyInfo> appliedStrategies = new ArrayList<>();
        
        public PriceCalculationContext(PriceCalculationRequest request, List<PriceStrategy> applicableStrategies) {
            this.request = request;
            this.applicableStrategies = applicableStrategies;
            this.originalPrice = request.getBasePrice();
            this.finalPrice = request.getBasePrice();
        }
        
        // getters and setters
        public PriceCalculationRequest getRequest() {
            return request;
        }
        
        public List<PriceStrategy> getApplicableStrategies() {
            return applicableStrategies;
        }
        
        public BigDecimal getOriginalPrice() {
            return originalPrice;
        }
        
        public BigDecimal getFinalPrice() {
            return finalPrice;
        }
        
        public void setFinalPrice(BigDecimal finalPrice) {
            this.finalPrice = finalPrice;
        }
        
        public BigDecimal getTotalDiscount() {
            return totalDiscount;
        }
        
        public void setTotalDiscount(BigDecimal totalDiscount) {
            this.totalDiscount = totalDiscount;
        }
        
        public BigDecimal getTotalDiscountRate() {
            return totalDiscountRate;
        }
        
        public void setTotalDiscountRate(BigDecimal totalDiscountRate) {
            this.totalDiscountRate = totalDiscountRate;
        }
        
        public List<PriceCalculationResult.AppliedStrategyInfo> getAppliedStrategies() {
            return appliedStrategies;
        }
        
        public void setAppliedStrategies(List<PriceCalculationResult.AppliedStrategyInfo> appliedStrategies) {
            this.appliedStrategies = appliedStrategies;
        }
    }
}