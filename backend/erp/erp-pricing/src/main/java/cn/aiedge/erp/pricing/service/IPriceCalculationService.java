package cn.aiedge.erp.pricing.service;

import cn.aiedge.erp.pricing.dto.PriceCalculationRequest;
import cn.aiedge.erp.pricing.dto.PriceCalculationResult;
import cn.aiedge.erp.pricing.dto.PriceStrategyDTO;

import java.util.List;

public interface IPriceCalculationService {
    PriceCalculationResult calculatePrice(PriceCalculationRequest request);
    List<PriceStrategyDTO> getAvailableStrategies(String customerId, String productId);
}