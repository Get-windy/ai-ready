package cn.aiedge.erp.pricing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class PriceCalculationRequest {
    private String productId;
    private String customerId;
    private String customerLevel;
    private String regionCode;
    private String productCategoryId;
    private String strategyType;
    private Integer quantity;
    private BigDecimal basePrice;
    private String calculationContext;
    private LocalDateTime calculationTime;
    private Map<String, Object> additionalParams;
}