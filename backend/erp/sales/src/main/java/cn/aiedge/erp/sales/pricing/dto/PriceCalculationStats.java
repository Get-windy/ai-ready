package cn.aiedge.erp.sales.pricing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 价格计算统计信息DTO
 */
@Data
public class PriceCalculationStats {
    private Long totalCalculations;
    private Long successfulCalculations;
    private Long failedCalculations;
    private BigDecimal averageCalculationTimeMs;
    private BigDecimal minCalculationTimeMs;
    private BigDecimal maxCalculationTimeMs;
    private Map<String, Long> calculationsByStrategy;
    private Long cacheHitCount;
    private Long cacheMissCount;
    private BigDecimal cacheHitRate;
}
