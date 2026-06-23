package cn.aiedge.erp.pricing.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class PriceCalculationResult {
    private BigDecimal finalPrice;
    private BigDecimal originalPrice;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private String calculationExplanation;
    private List<PriceAdjustmentDetail> adjustmentDetails;
    private Map<String, Object> calculationMetadata;
    private String errorMessage;
    private Boolean success;

    @Data
    public static class PriceAdjustmentDetail {
        private String ruleName;
        private String adjustmentType;
        private BigDecimal adjustmentValue;
        private String explanation;
    }
}