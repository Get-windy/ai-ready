package cn.aiedge.erp.monitor.enums;

import lombok.Getter;

/**
 * 指标状态枚举
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public enum MetricStatus {

    NORMAL("normal", "正常", "success"),
    WARNING("warning", "警告", "warning"),
    CRITICAL("critical", "严重", "danger"),
    UNKNOWN("unknown", "未知", "info");

    private final String code;
    private final String name;
    private final String level;

    MetricStatus(String code, String name, String level) {
        this.code = code;
        this.name = name;
        this.level = level;
    }

    public static MetricStatus fromCode(String code) {
        for (MetricStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return UNKNOWN;
    }
}
