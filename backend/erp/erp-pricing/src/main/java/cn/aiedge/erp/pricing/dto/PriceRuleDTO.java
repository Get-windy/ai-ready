package cn.aiedge.erp.pricing.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PriceRuleDTO {
    private Long id;
    private Long strategyId;
    private String ruleName;
    private String ruleType;
    private String conditionType;
    private String conditionValue;
    private String calculationType;
    private BigDecimal priceFactor;
    private BigDecimal discountRate;
    private BigDecimal discountAmount;
    private Integer minQuantity;
    private Integer maxQuantity;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Integer priority;
    private String status;
}