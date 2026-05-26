package cn.aiedge.erp.price.engine.config.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 价格条件DTO
 * 表示价格策略的触发条件
 */
public record PriceCondition(
        String conditionId,
        ConditionType conditionType,
        String field,
        Operator operator,
        String value,
        BigDecimal numericValue,
        LocalDateTime dateValue,
        Boolean booleanValue,
        String description
) {
    public enum ConditionType {
        CUSTOMER_ATTRIBUTE,     // 客户属性
        PRODUCT_ATTRIBUTE,      // 产品属性
        ORDER_ATTRIBUTE,        // 订单属性
        TIME_BASED,             // 时间条件
        QUANTITY_BASED,         // 数量条件
        REGIONAL,              // 区域条件
        CHANNEL_BASED          // 渠道条件
    }
    
    public enum Operator {
        EQUALS,                 // 等于
        NOT_EQUALS,             // 不等于
        GREATER_THAN,           // 大于
        GREATER_THAN_OR_EQUALS, // 大于等于
        LESS_THAN,              // 小于
        LESS_THAN_OR_EQUALS,    // 小于等于
        BETWEEN,                // 在...之间
        IN,                     // 在列表中
        NOT_IN,                 // 不在列表中
        CONTAINS,               // 包含
        STARTS_WITH,            // 以...开始
        ENDS_WITH               // 以...结束
    }
}