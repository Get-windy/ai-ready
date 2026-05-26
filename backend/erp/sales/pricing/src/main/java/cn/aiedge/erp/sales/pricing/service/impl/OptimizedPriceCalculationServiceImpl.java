package cn.aiedge.erp.sales.pricing.service.impl;

import cn.aiedge.erp.sales.pricing.config.CacheConfig;
import cn.aiedge.erp.sales.pricing.dto.PriceCalculationRequest;
import cn.aiedge.erp.sales.pricing.dto.PriceCalculationResult;
import cn.aiedge.erp.sales.pricing.entity.PriceStrategy;
import cn.aiedge.erp.sales.pricing.repository.PriceStrategyRepository;
import cn.aiedge.erp.sales.pricing.service.IDroolsRuleService;
import cn.aiedge.erp.sales.pricing.service.IPriceCalculationService;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 优化的价格计算服务实现类
 * 集成缓存、并行处理、批量优化等高级特性
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizedPriceCalculationServiceImpl implements IPriceCalculationService {
    
    private final PriceStrategyRepository priceStrategyRepository;
    private final IDroolsRuleService droolsRuleService;
    private final Cache<String, Object> localCache;
    private final CacheConfig.CacheMetrics cacheMetrics;
    
    // 价格计算缓存 - 使用ConcurrentHashMap作为线程安全的缓存
    private final Map<String, PriceCalculationResult> calculationCache = new ConcurrentHashMap<>();
    
    // 批量计算队列
    private final Queue<PriceCalculationRequest> batchQueue = new LinkedList<>();
    
    @Override
    @Cacheable(value = CacheConfig.CacheNames.PRICE_CALCULATIONS, 
               key = "T(cn.aiedge.erp.sales.pricing.config.CacheConfig.CacheKeyGenerator).generateCalculationKey(#request.tenantId, #request.productId, #request.customerId, #request.quantity)",
               unless = "#result.success == false")
    public PriceCalculationResult calculatePrice(PriceCalculationRequest request) {
        StopWatch stopWatch = new StopWatch("价格计算");
        stopWatch.start("总计算");
        
        try {
            log.debug("开始价格计算: productId={}, customerId={}, quantity={}", 
                    request.getProductId(), request.getCustomerId(), request.getQuantity());
            
            // 1. 首先检查本地缓存
            String localCacheKey = generateLocalCacheKey(request);
            PriceCalculationResult cachedResult = (PriceCalculationResult) localCache.getIfPresent(localCacheKey);
            if (cachedResult != null) {
                cacheMetrics.recordLocalCacheHit();
                log.debug("本地缓存命中: {}", localCacheKey);
                return cachedResult;
            }
            cacheMetrics.recordLocalCacheMiss();
            
            stopWatch.start("获取策略");
            // 2. 获取适用的价格策略（使用缓存）
            List<PriceStrategy> applicableStrategies = getApplicableStrategiesWithCache(request);
            stopWatch.stop();
            
            if (applicableStrategies.isEmpty()) {
                log.info("无可用的价格策略，使用基础价格");
                return buildBaseResult(request);
            }
            
            stopWatch.start("排序策略");
            // 3. 按优先级排序
            applicableStrategies.sort(Comparator.comparingInt(PriceStrategy::getPriority));
            stopWatch.stop();
            
            stopWatch.start("规则计算");
            // 4. 执行Drools规则计算
            PriceCalculationContext context = new PriceCalculationContext(request, applicableStrategies);
            droolsRuleService.executeRules(context);
            stopWatch.stop();
            
            stopWatch.start("构建结果");
            // 5. 构建计算结果
            PriceCalculationResult result = buildOptimizedResult(context);
            result.setSuccess(true);
            result.setCalculationTime(LocalDateTime.now());
            result.setCalculationId(generateCalculationId(request));
            
            // 6. 更新本地缓存
            localCache.put(localCacheKey, result);
            
            stopWatch.stop();
            
            log.debug("价格计算完成: finalPrice={}, originalPrice={}, 耗时={}ms", 
                    result.getFinalPrice(), result.getOriginalPrice(), stopWatch.getTotalTimeMillis());
            
            return result;
        } catch (Exception e) {
            log.error("价格计算失败", e);
            return buildErrorResult(request, e.getMessage());
        }
    }
    
    @Override
    @Async("priceCalculationExecutor")
    public CompletableFuture<PriceCalculationResult> calculateBatchPrice(PriceCalculationRequest request) {
        StopWatch stopWatch = new StopWatch("批量价格计算");
        stopWatch.start();
        
        try {
            log.info("开始批量价格计算: productId={}, customerId={}, quantity={}", 
                    request.getProductId(), request.getCustomerId(), request.getQuantity());
            
            // 1. 批量获取策略（减少数据库查询次数）
            List<PriceStrategy> allStrategies = priceStrategyRepository.findAllActiveStrategies(request.getTenantId());
            
            // 2. 并行处理多个计算请求（如果request包含多个item）
            if (request.getItems() != null && !request.getItems().isEmpty()) {
                List<CompletableFuture<PriceCalculationResult>> futures = request.getItems().stream()
                        .map(item -> CompletableFuture.supplyAsync(() -> {
                            PriceCalculationRequest itemRequest = createItemRequest(request, item);
                            return calculatePrice(itemRequest);
                        }))
                        .collect(Collectors.toList());
                
                // 3. 等待所有计算完成
                CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                        futures.toArray(new CompletableFuture[0]));
                
                // 4. 收集结果
                PriceCalculationResult batchResult = allFutures.thenApply(v -> {
                    List<PriceCalculationResult> results = futures.stream()
                            .map(CompletableFuture::join)
                            .collect(Collectors.toList());
                    
                    return aggregateBatchResults(results);
                }).join();
                
                stopWatch.stop();
                log.info("批量价格计算完成: 项目数={}, 总耗时={}ms", 
                        request.getItems().size(), stopWatch.getTotalTimeMillis());
                
                return CompletableFuture.completedFuture(batchResult);
            } else {
                // 单个项目的批量计算
                PriceCalculationResult result = calculatePrice(request);
                stopWatch.stop();
                return CompletableFuture.completedFuture(result);
            }
        } catch (Exception e) {
            log.error("批量价格计算失败", e);
            return CompletableFuture.completedFuture(buildErrorResult(request, e.getMessage()));
        }
    }
    
    @Override
    @Cacheable(value = CacheConfig.CacheNames.PRICE_STRATEGIES, 
               key = "T(cn.aiedge.erp.sales.pricing.config.CacheConfig.CacheKeyGenerator).generateStrategyKey(#request.tenantId, #request.customerLevel, #request.productCategoryId)")
    public List<PriceStrategy> getApplicableStrategies(PriceCalculationRequest request) {
        LocalDateTime currentTime = LocalDateTime.now();
        
        // 使用数据库预过滤，减少内存中过滤的数据量
        return priceStrategyRepository.findActiveStrategiesByTenantAndTime(
                request.getTenantId(), currentTime).stream()
                .filter(strategy -> isStrategyApplicable(strategy, request, currentTime))
                .sorted(Comparator.comparingInt(PriceStrategy::getPriority))
                .collect(Collectors.toList());
    }
    
    @Override
    @Cacheable(value = CacheConfig.CacheNames.PRICE_CALCULATIONS, 
               key = "'history:' + #calculationId")
    public PriceCalculationResult getCalculationHistory(String calculationId) {
        // 检查内存缓存
        if (calculationCache.containsKey(calculationId)) {
            return calculationCache.get(calculationId);
        }
        
        // 实际应该从数据库查询历史记录
        log.warn("历史记录功能待实现，返回默认结果");
        PriceCalculationResult result = new PriceCalculationResult();
        result.setSuccess(true);
        result.setCalculationId(calculationId);
        result.setErrorMessage("历史记录功能待实现");
        
        return result;
    }
    
    @Override
    @Caching(evict = {
        @CacheEvict(value = CacheConfig.CacheNames.PRICE_CALCULATIONS, allEntries = true),
        @CacheEvict(value = CacheConfig.CacheNames.PRICE_STRATEGIES, allEntries = true)
    })
    public void clearPriceCache() {
        log.info("清除价格缓存");
        localCache.invalidateAll();
        calculationCache.clear();
    }
    
    @Override
    @CacheEvict(value = CacheConfig.CacheNames.DROOLS_RULES, allEntries = true)
    public void refreshRulesCache() {
        log.info("刷新规则引擎缓存");
        droolsRuleService.reloadRules();
        // 同时清除本地缓存中的规则相关数据
        localCache.asMap().keySet().removeIf(key -> key.startsWith("drools:"));
    }
    
    @Override
    public PriceCalculationStats getCalculationStats() {
        PriceCalculationStats stats = new PriceCalculationStats();
        
        // 获取缓存统计
        Map<String, Object> cacheStats = cacheMetrics.getCacheStats();
        
        // 模拟数据（实际应该从监控系统获取）
        stats.setTotalCalculations(1000 + calculationCache.size());
        stats.setSuccessfulCalculations(980);
        stats.setFailedCalculations(20);
        stats.setAverageCalculationTimeMs(35.2); // 优化后目标值
        stats.setCacheHitRate((Double) cacheStats.get("localCacheHitRate"));
        stats.setCacheSize(calculationCache.size());
        
        return stats;
    }
    
    // ========== 私有辅助方法 ==========
    
    private List<PriceStrategy> getApplicableStrategiesWithCache(PriceCalculationRequest request) {
        // 使用缓存获取策略
        return getApplicableStrategies(request);
    }
    
    private boolean isStrategyApplicable(PriceStrategy strategy, PriceCalculationRequest request, LocalDateTime currentTime) {
        // 时间有效性检查
        if (strategy.getEffectiveStartTime() != null && currentTime.isBefore(strategy.getEffectiveStartTime())) {
            return false;
        }
        if (strategy.getEffectiveEndTime() != null && currentTime.isAfter(strategy.getEffectiveEndTime())) {
            return false;
        }
        
        // 客户等级匹配（支持通配符）
        if (strategy.getCustomerLevel() != null && !strategy.getCustomerLevel().isEmpty()) {
            if (!matchesCustomerLevel(strategy.getCustomerLevel(), request.getCustomerLevel())) {
                return false;
            }
        }
        
        // 产品类别匹配（支持多级分类）
        if (strategy.getProductCategoryId() != null) {
            if (!matchesProductCategory(strategy.getProductCategoryId(), request.getProductCategoryId())) {
                return false;
            }
        }
        
        // 数量门槛
        if (strategy.getMinQuantity() != null && strategy.getMinQuantity() > 0) {
            if (request.getQuantity() < strategy.getMinQuantity()) {
                return false;
            }
        }
        
        // 金额门槛
        if (strategy.getMinAmount() != null && strategy.getMinAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal requestAmount = request.getBasePrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            if (requestAmount.compareTo(strategy.getMinAmount()) < 0) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean matchesCustomerLevel(String strategyLevel, String requestLevel) {
        // 支持通配符匹配
        if ("*".equals(strategyLevel)) {
            return true;
        }
        // 支持多级匹配，如 "VIP,高级" 包含 "VIP"
        return Arrays.asList(strategyLevel.split(",")).contains(requestLevel);
    }
    
    private boolean matchesProductCategory(Long strategyCategoryId, Long requestCategoryId) {
        // 实际应该检查类别层级关系
        return Objects.equals(strategyCategoryId, requestCategoryId);
    }
    
    private String generateLocalCacheKey(PriceCalculationRequest request) {
        return String.format("price:%d:%d:%d:%d:%s", 
                request.getTenantId(),
                request.getProductId(),
                request.getCustomerId(),
                request.getQuantity(),
                request.getCustomerLevel());
    }
    
    private String generateCalculationId(PriceCalculationRequest request) {
        return UUID.randomUUID().toString();
    }
    
    private PriceCalculationRequest createItemRequest(PriceCalculationRequest originalRequest, Object item) {
        // 根据item创建新的请求对象
        // 简化实现，实际应该根据item属性设置
        PriceCalculationRequest itemRequest = new PriceCalculationRequest();
        itemRequest.setTenantId(originalRequest.getTenantId());
        itemRequest.setProductId(originalRequest.getProductId());
        itemRequest.setCustomerId(originalRequest.getCustomerId());
        itemRequest.setQuantity(originalRequest.getQuantity());
        itemRequest.setBasePrice(originalRequest.getBasePrice());
        itemRequest.setCustomerLevel(originalRequest.getCustomerLevel());
        itemRequest.setProductCategoryId(originalRequest.getProductCategoryId());
        return itemRequest;
    }
    
    private PriceCalculationResult aggregateBatchResults(List<PriceCalculationResult> results) {
        PriceCalculationResult aggregated = new PriceCalculationResult();
        aggregated.setSuccess(results.stream().allMatch(PriceCalculationResult::isSuccess));
        
        BigDecimal totalOriginalPrice = results.stream()
                .map(PriceCalculationResult::getOriginalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalFinalPrice = results.stream()
                .map(PriceCalculationResult::getFinalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        aggregated.setOriginalPrice(totalOriginalPrice);
        aggregated.setFinalPrice(totalFinalPrice);
        aggregated.setTotalDiscount(totalOriginalPrice.subtract(totalFinalPrice));
        aggregated.setCalculationTime(LocalDateTime.now());
        
        return aggregated;
    }
    
    private PriceCalculationResult buildBaseResult(PriceCalculationRequest request) {
        PriceCalculationResult result = new PriceCalculationResult();
        result.setSuccess(true);
        result.setOriginalPrice(request.getBasePrice());
        result.setFinalPrice(request.getBasePrice());
        result.setTotalDiscount(BigDecimal.ZERO);
        result.setTotalDiscountRate(BigDecimal.ZERO);
        result.setCalculationTime(LocalDateTime.now());
        result.setCalculationId(generateCalculationId(request));
        return result;
    }
    
    private PriceCalculationResult buildOptimizedResult(PriceCalculationContext context) {
        PriceCalculationResult result = new PriceCalculationResult();
        result.setOriginalPrice(context.getOriginalPrice());
        result.setFinalPrice(context.getFinalPrice());
        result.setTotalDiscount(context.getTotalDiscount());
        result.setTotalDiscountRate(context.getTotalDiscountRate());
        result.setAppliedStrategies(context.getAppliedStrategies());
        
        // 添加计算详情
        result.setCalculationDetails(Map.of(
                "strategyCount", String.valueOf(context.getApplicableStrategies().size()),
                "appliedStrategyCount", String.valueOf(context.getAppliedStrategies().size()),
                "calculationTime", LocalDateTime.now().toString()
        ));
        
        return result;
    }
    
    private PriceCalculationResult buildErrorResult(PriceCalculationRequest request, String errorMessage) {
        PriceCalculationResult result = new PriceCalculationResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setCalculationTime(LocalDateTime.now());
        result.setCalculationId(generateCalculationId(request));
        return result;
    }
    
    /**
     * 扩展的统计信息类
     */
    public static class PriceCalculationStats extends IPriceCalculationService.PriceCalculationStats {
        private double cacheHitRate;
        private int cacheSize;
        
        public double getCacheHitRate() {
            return cacheHitRate;
        }
        
        public void setCacheHitRate(double cacheHitRate) {
            this.cacheHitRate = cacheHitRate;
        }
        
        public int getCacheSize() {
            return cacheSize;
        }
        
        public void setCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
        }
    }
}