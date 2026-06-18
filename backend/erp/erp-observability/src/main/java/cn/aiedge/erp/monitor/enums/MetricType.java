package cn.aiedge.erp.monitor.enums;

import lombok.Getter;

/**
 * 指标类型枚举
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public enum MetricType {

    ORDER("order", "订单指标", "统计订单相关数据"),
    INVENTORY("inventory", "库存指标", "统计库存相关数据"),
    USER("user", "用户指标", "统计用户相关数据"),
    SALES("sales", "销售指标", "统计销售相关数据"),
    PURCHASE("purchase", "采购指标", "统计采购相关数据"),
    FINANCE("finance", "财务指标", "统计财务相关数据"),
    SYSTEM("system", "系统指标", "统计系统运行数据");

    private final String code;
    private final String name;
    private final String description;

    MetricType(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    public static MetricType fromCode(String code) {
        for (MetricType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
