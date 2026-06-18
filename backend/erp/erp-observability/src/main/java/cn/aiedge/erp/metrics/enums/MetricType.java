package cn.aiedge.erp.metrics.enums;

import lombok.Getter;

/**
 * 指标类型枚举
 */
@Getter
public enum MetricType {
    ORDER("order", "订单指标"),
    INVENTORY("inventory", "库存指标"),
    USER("user", "用户指标"),
    SALES("sales", "销售指标"),
    FINANCE("finance", "财务指标");
    
    private final String code;
    private final String description;
    
    MetricType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public static MetricType fromCode(String code) {
        for (MetricType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown metric type: " + code);
    }
}
