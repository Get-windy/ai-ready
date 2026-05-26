package cn.aiedge.erp.price.engine.service;

import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationRequest;
import cn.aiedge.erp.price.engine.strategy.entity.PriceCalculationResult;

import java.util.List;
import java.util.Map;

public interface PriceEngineService {
    
    PriceCalculationResult calculatePrice(PriceCalculationRequest request);
    
    List<PriceCalculationResult> calculateBatchPrices(List<PriceCalculationRequest> requests);
    
    PriceCalculationResult simulatePrice(PriceCalculationRequest request, String strategyId);
    
    Map<String, Object> getPriceHistory(String productId, String customerId, int days);
    
    List<String> getApplicableStrategies(PriceCalculationRequest request);
    
    boolean validatePriceConfiguration(String strategyId);
    
    Map<String, Object> getPriceStatistics(String productId);
    
    PriceCalculationResult getOptimalPrice(PriceCalculationRequest request);
    
    void refreshPriceCache();
    
    Map<String, Object> getEngineHealth();
}