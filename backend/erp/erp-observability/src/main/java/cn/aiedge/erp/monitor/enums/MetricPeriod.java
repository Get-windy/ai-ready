package cn.aiedge.erp.monitor.enums;

import lombok.Getter;

/**
 * 指标周期枚举
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Getter
public enum MetricPeriod {

    REALTIME("realtime", "实时", 0),
    MINUTE_1("1m", "1分钟", 1),
    MINUTE_5("5m", "5分钟", 5),
    MINUTE_15("15m", "15分钟", 15),
    HOUR_1("1h", "1小时", 60),
    HOUR_4("4h", "4小时", 240),
    DAY_1("1d", "1天", 1440),
    WEEK_1("1w", "1周", 10080),
    MONTH_1("1M", "1月", 43200);

    private final String code;
    private final String name;
    private final int minutes;

    MetricPeriod(String code, String name, int minutes) {
        this.code = code;
        this.name = name;
        this.minutes = minutes;
    }

    public static MetricPeriod fromCode(String code) {
        for (MetricPeriod period : values()) {
            if (period.getCode().equals(code)) {
                return period;
            }
        }
        return null;
    }
}
