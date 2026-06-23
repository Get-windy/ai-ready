package cn.aiedge.erp.pricing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PriceStrategyDTO {
    private Long id;
    private Long tenantId;
    private String name;
    private String description;
    private String strategyType;
    private String customerLevel;
    private String regionCode;
    private Long productCategoryId;
    private BigDecimal basePrice;
    private BigDecimal priceFactor;
    private BigDecimal discountRate;
    private BigDecimal discountAmount;
    private Integer minQuantity;
    private String formulaConfig;
    private LocalDateTime effectiveStartTime;
    private LocalDateTime effectiveEndTime;
    private Integer priority;
    private String status;
}