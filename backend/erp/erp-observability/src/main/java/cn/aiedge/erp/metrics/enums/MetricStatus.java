package cn.aiedge.erp.metrics.enums;

import lombok.Getter;

/**
 * 指标状态枚举
 */
@Getter
public enum MetricStatus {
    ACTIVE("active", "活跃"),
    INACTIVE("inactive", "停用"),
    DEPRECATED("deprecated", "已废弃");
    
    private final String code;
    private final String description;
    
    MetricStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
