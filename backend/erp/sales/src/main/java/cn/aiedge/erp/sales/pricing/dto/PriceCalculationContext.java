package cn.aiedge.erp.sales.pricing.dto;

import cn.aiedge.erp.sales.pricing.entity.PriceStrategy;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 价格计算上下文
 */
@Data
public class PriceCalculationContext {
    private PriceCalculationRequest request;
    private BigDecimal originalPrice;
    private BigDecimal finalPrice;
    private BigDecimal totalDiscount;
    private BigDecimal totalDiscountRate;
    private List<PriceCalculationResult.AppliedStrategyInfo> appliedStrategies;
    private List<PriceStrategyDTO> applicableStrategies;
    private Map<String, Object> calculationData;

    public PriceCalculationContext() {}

    public PriceCalculationContext(PriceCalculationRequest request, List<PriceStrategy> strategies) {
        this.request = request;
        this.originalPrice = request.getBasePrice();
        this.finalPrice = request.getBasePrice();
        this.totalDiscount = BigDecimal.ZERO;
        this.totalDiscountRate = BigDecimal.ZERO;
    }
}
