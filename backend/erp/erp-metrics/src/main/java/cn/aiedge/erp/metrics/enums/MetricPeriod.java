package cn.aiedge.erp.metrics.enums;

import lombok.Getter;

/**
 * 指标周期枚举
 */
@Getter
public enum MetricPeriod {
    REALTIME("realtime", "实时", 0),
    MINUTE_1("1m", "1分钟", 60),
    MINUTE_5("5m", "5分钟", 300),
    MINUTE_15("15m", "15分钟", 900),
    HOUR_1("1h", "1小时", 3600),
    DAY_1("1d", "1天", 86400),
    WEEK_1("1w", "1周", 604800);
    
    private final String code;
    private final String description;
    private final int seconds;
    
    MetricPeriod(String code, String description, int seconds) {
        this.code = code;
        this.description = description;
        this.seconds = seconds;
    }
    
    public static MetricPeriod fromCode(String code) {
        for (MetricPeriod period : values()) {
            if (period.code.equals(code)) {
                return period;
            }
        }
        throw new IllegalArgumentException("Unknown metric period: " + code);
    }
}
