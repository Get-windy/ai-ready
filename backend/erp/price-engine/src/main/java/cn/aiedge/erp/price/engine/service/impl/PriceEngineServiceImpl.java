package cn.aiedge.erp.price.engine.service.impl;

import cn.aiedge.erp.price.engine.config.IPriceRuleEngine;
import cn.aiedge.erp.price.engine.config.dto.PriceRule;
import cn.aiedge.erp.price.engine.optimization.IPriceOptimizationAlgorithm;
import cn.aiedge.erp.price.engine.optimization.dto.OptimizationRequest;
import cn.aiedge.erp.price.engine.optimization.dto.OptimizationResult;
import cn.aiedge.erp.price.engine.service.PriceEngineService;
import cn.aiedge.erp.price.engine.strategy.DiscountEngine;
import cn.aiedge.erp.price.engine.strategy.entity.DiscountRule;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationResult;
import cn.aiedge.erp.price.engine.strategy.factory.PricingStrategyFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PriceEngineServiceImpl implements PriceEngineService {
    
    private final IPriceRuleEngine ruleEngine;
    private final DiscountEngine discountEngine;
    private final PricingStrategyFactory strategyFactory;
    private final IPriceOptimizationAlgorithm optimizationAlgorithm;
    
    private final Map<String, PriceCalculationResult> priceCache = new ConcurrentHashMap<>();
    private final Map<String, List<PriceCalculationResult>> priceHistoryMap = new ConcurrentHashMap<>();
    
    public PriceEngineServiceImpl(IPriceRuleEngine ruleEngine, 
                                  DiscountEngine discountEngine,
                                  PricingStrategyFactory strategyFactory,
                                  IPriceOptimizationAlgorithm optimizationAlgorithm) {
        this.ruleEngine = ruleEngine;
        this.discountEngine = discountEngine;
        this.strategyFactory = strategyFactory;
        this.optimizationAlgorithm = optimizationAlgorithm;
    }
    
    @Override
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request) {
        String resultId = UUID.randomUUID().toString();
        PriceCalculationResult result = new PriceCalculationResult(resultId, request.getRequestId());
        
        try {
            if (!request.isValid()) {
                result.setError("INVALID_REQUEST", "请求参数无效");
                return result;
            }
            
            BigDecimal basePrice = request.getEffectiveBasePrice();
            result.setOriginalBasePrice(basePrice);
            result.setBasePrice(basePrice);
            
            Map<String, Object> context = buildContext(request);
            
            List<PriceRule> matchingRules = ruleEngine.getMatchingRules(context);
            
            if (!matchingRules.isEmpty()) {
                double adjustedPrice = ruleEngine.applyRules(basePrice.doubleValue(), context);
                result.setBasePrice(BigDecimal.valueOf(adjustedPrice).setScale(2, RoundingMode.HALF_UP));
            }
            
            List<DiscountRule> discountRules = getDiscountRules(request);
            List<DiscountRule> applicableRules = discountEngine.findApplicableDiscountRules(
                    discountRules, request, result.getBasePrice());
            
            BigDecimal totalDiscount = discountEngine.calculateTotalDiscount(
                    applicableRules, request, result.getBasePrice(), result);
            
            result.setTotalDiscountAmount(totalDiscount);
            result.calculateFinalPrice(request.getQuantity());
            
            result.setCostPrice(request.getCostPrice());
            
            if (request.getCostPrice() != null && request.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal margin = result.getFinalPrice().subtract(request.getCostPrice())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(request.getCostPrice(), 2, RoundingMode.HALF_UP);
                result.setGrossProfitMargin(margin);
            }
            
            result.setEngineVersion("1.0.0");
            result.completeCalculation();
            
            String cacheKey = buildCacheKey(request);
            priceCache.put(cacheKey, result);
            
            addToHistory(request.getProductId(), result);
            
            return result;
        } catch (Exception e) {
            result.setError("CALCULATION_ERROR", "价格计算异常: " + e.getMessage());
            return result;
        }
    }
    
    @Override
    public List<PriceCalculationResult> calculateBatchPrices(List<PriceCalculationRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        
        return requests.stream()
                .map(this::calculatePrice)
                .collect(Collectors.toList());
    }
    
    @Override
    public PriceCalculationResult simulatePrice(PriceCalculationRequest request, String strategyId) {
        PriceCalculationResult result = calculatePrice(request);
        
        result.setCalculationExplanation("模拟计算 - 策略ID: " + strategyId);
        result.setSuggestion("这是模拟计算结果，实际价格可能有所不同");
        
        return result;
    }
    
    @Override
    public Map<String, Object> getPriceHistory(String productId, String customerId, int days) {
        Map<String, Object> history = new HashMap<>();
        
        String key = productId + "_" + customerId;
        List<PriceCalculationResult> results = priceHistoryMap.getOrDefault(key, new ArrayList<>());
        
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        List<PriceCalculationResult> filteredResults = results.stream()
                .filter(r -> r.getCalculationStartTime().isAfter(threshold))
                .sorted((r1, r2) -> r2.getCalculationStartTime().compareTo(r1.getCalculationStartTime()))
                .collect(Collectors.toList());
        
        history.put("productId", productId);
        history.put("customerId", customerId);
        history.put("days", days);
        history.put("totalRecords", filteredResults.size());
        history.put("records", filteredResults);
        
        if (!filteredResults.isEmpty()) {
            BigDecimal avgPrice = filteredResults.stream()
                    .map(PriceCalculationResult::getFinalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(filteredResults.size()), 2, RoundingMode.HALF_UP);
            
            BigDecimal maxPrice = filteredResults.stream()
                    .map(PriceCalculationResult::getFinalPrice)
                    .max(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            BigDecimal minPrice = filteredResults.stream()
                    .map(PriceCalculationResult::getFinalPrice)
                    .min(BigDecimal::compareTo)
                    .orElse(BigDecimal.ZERO);
            
            history.put("averagePrice", avgPrice);
            history.put("maxPrice", maxPrice);
            history.put("minPrice", minPrice);
        }
        
        return history;
    }
    
    @Override
    public List<String> getApplicableStrategies(PriceCalculationRequest request) {
        Map<String, Object> context = buildContext(request);
        List<PriceRule> matchingRules = ruleEngine.getMatchingRules(context);
        
        return matchingRules.stream()
                .map(PriceRule::strategyId)
                .distinct()
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean validatePriceConfiguration(String strategyId) {
        return strategyId != null && !strategyId.isEmpty();
    }
    
    @Override
    public Map<String, Object> getPriceStatistics(String productId) {
        Map<String, Object> stats = new HashMap<>();
        
        List<PriceCalculationResult> allResults = priceHistoryMap.values().stream()
                .flatMap(List::stream)
                .filter(r -> r.getSuccess())
                .collect(Collectors.toList());
        
        stats.put("productId", productId);
        stats.put("totalCalculations", allResults.size());
        
        if (!allResults.isEmpty()) {
            BigDecimal avgDiscount = allResults.stream()
                    .map(PriceCalculationResult::getTotalDiscountAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(allResults.size()), 2, RoundingMode.HALF_UP);
            
            stats.put("averageDiscount", avgDiscount);
            
            long avgDuration = allResults.stream()
                    .mapToLong(r -> r.getCalculationDurationMs() != null ? r.getCalculationDurationMs() : 0)
                    .average()
                    .orElse(0);
            
            stats.put("averageCalculationTimeMs", avgDuration);
        }
        
        return stats;
    }
    
    @Override
    public PriceCalculationResult getOptimalPrice(PriceCalculationRequest request) {
        OptimizationRequest optRequest = new OptimizationRequest(
                UUID.randomUUID().toString(),
                request.getProductId(),
                request.getCustomerId(),
                request.getBasePrice(),
                request.getCostPrice(),
                request.getQuantity(),
                request.getCompetitorPrices(),
                "MAXIMIZE_PROFIT",
                new HashMap<>()
        );
        
        OptimizationResult optResult = optimizationAlgorithm.optimize(optRequest);
        
        PriceCalculationResult result = calculatePrice(request);
        
        if (optResult != null && optResult.recommendedPrice() != null) {
            result.setFinalPrice(optResult.recommendedPrice());
            result.setSuggestion("优化建议: " + optResult.recommendation());
        }
        
        return result;
    }
    
    @Override
    public void refreshPriceCache() {
        priceCache.clear();
    }
    
    @Override
    public Map<String, Object> getEngineHealth() {
        Map<String, Object> health = new HashMap<>();
        
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("cacheSize", priceCache.size());
        health.put("historySize", priceHistoryMap.size());
        
        IPriceRuleEngine.RuleExecutionStats ruleStats = ruleEngine.getExecutionStats();
        health.put("ruleEngineStats", Map.of(
                "totalExecutions", ruleStats.totalExecutions(),
                "successfulExecutions", ruleStats.successfulExecutions(),
                "failedExecutions", ruleStats.failedExecutions(),
                "averageExecutionTimeMs", ruleStats.averageExecutionTimeMs()
        ));
        
        return health;
    }
    
    private Map<String, Object> buildContext(PriceCalculationRequest request) {
        Map<String, Object> context = new HashMap<>();
        
        context.put("productId", request.getProductId());
        context.put("customerId", request.getCustomerId());
        context.put("quantity", request.getQuantity());
        context.put("basePrice", request.getBasePrice());
        context.put("customerLevel", request.getCustomerLevel());
        context.put("region", request.getRegion());
        context.put("salesChannel", request.getSalesChannel());
        context.put("category", request.getCategory());
        context.put("timestamp", LocalDateTime.now());
        
        if (request.getAdditionalParams() != null) {
            context.putAll(request.getAdditionalParams());
        }
        
        return context;
    }
    
    private List<DiscountRule> getDiscountRules(PriceCalculationRequest request) {
        List<DiscountRule> rules = new ArrayList<>();
        
        if (request.getCustomerLevel() != null) {
            DiscountRule levelRule = createCustomerLevelDiscountRule(request);
            rules.add(levelRule);
        }
        
        if (request.getQuantity() != null && request.getQuantity() >= 10) {
            DiscountRule volumeRule = createVolumeDiscountRule(request);
            rules.add(volumeRule);
        }
        
        return rules;
    }
    
    private DiscountRule createCustomerLevelDiscountRule(PriceCalculationRequest request) {
        BigDecimal discountValue = getLevelDiscountValue(request.getCustomerLevel());
        
        DiscountRule rule = new DiscountRule();
        rule.setRuleId("level_" + request.getCustomerLevel());
        rule.setRuleName("客户等级折扣");
        rule.setDiscountType(DiscountRule.DiscountType.STANDARD);
        rule.setDiscountRate(discountValue);
        rule.setEnabled(true);
        rule.setPriority(10);
        rule.setEffectiveFrom(LocalDateTime.now());
        rule.setEffectiveTo(LocalDateTime.now().plusYears(1));
        rule.setStackable(true);
        rule.setApplicableCustomerLevels(List.of(request.getCustomerLevel()));
        
        return rule;
    }
    
    private DiscountRule createVolumeDiscountRule(PriceCalculationRequest request) {
        BigDecimal discountValue = BigDecimal.valueOf(5);
        if (request.getQuantity() >= 50) {
            discountValue = BigDecimal.valueOf(10);
        } else if (request.getQuantity() >= 100) {
            discountValue = BigDecimal.valueOf(15);
        }
        
        DiscountRule rule = new DiscountRule();
        rule.setRuleId("volume_" + request.getQuantity());
        rule.setRuleName("数量折扣");
        rule.setDiscountType(DiscountRule.DiscountType.VOLUME);
        rule.setDiscountRate(discountValue);
        rule.setMinPurchaseQuantity(request.getQuantity());
        rule.setEnabled(true);
        rule.setPriority(20);
        rule.setEffectiveFrom(LocalDateTime.now());
        rule.setEffectiveTo(LocalDateTime.now().plusYears(1));
        rule.setStackable(true);
        
        return rule;
    }
    
    private BigDecimal getLevelDiscountValue(String level) {
        switch (level) {
            case "VIP":
                return BigDecimal.valueOf(15);
            case "GOLD":
                return BigDecimal.valueOf(10);
            case "SILVER":
                return BigDecimal.valueOf(5);
            case "BRONZE":
                return BigDecimal.valueOf(3);
            default:
                return BigDecimal.ZERO;
        }
    }
    
    private String buildCacheKey(PriceCalculationRequest request) {
        return String.format("%s_%s_%d_%s",
                request.getProductId(),
                request.getCustomerId(),
                request.getQuantity(),
                request.getCustomerLevel());
    }
    
    private void addToHistory(String productId, PriceCalculationResult result) {
        String key = productId + "_all";
        List<PriceCalculationResult> history = priceHistoryMap.computeIfAbsent(key, k -> new ArrayList<>());
        history.add(result);
        
        if (history.size() > 1000) {
            history = history.stream()
                    .sorted((r1, r2) -> r2.getCalculationStartTime().compareTo(r1.getCalculationStartTime()))
                    .limit(500)
                    .collect(Collectors.toList());
            priceHistoryMap.put(key, history);
        }
    }
}