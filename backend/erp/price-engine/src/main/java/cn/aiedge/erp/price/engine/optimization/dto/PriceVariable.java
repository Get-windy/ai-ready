package cn.aiedge.erp.price.engine.optimization.dto;

import java.math.BigDecimal;

public record PriceVariable(
        String productId,
        String variableName,
        BigDecimal currentValue,
        BigDecimal minValue,
        BigDecimal maxValue,
        BigDecimal stepSize,
        VariableType variableType
) {
    public enum VariableType {
        BASE_PRICE,
        DISCOUNT_PERCENTAGE,
        PROMOTIONAL_AMOUNT,
        TIER_THRESHOLD,
        CUSTOM
    }
}