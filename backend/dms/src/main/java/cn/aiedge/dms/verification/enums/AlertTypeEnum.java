package cn.aiedge.dms.verification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 核验异常告警类型枚举
 */
@Getter
@AllArgsConstructor
public enum AlertTypeEnum {

    POSITION_MISMATCH(1, "人车位置分离异常"),
    ABNORMAL_STAY(2, "异常滞留"),
    SPEED_ANOMALY(3, "速度异常"),
    OFF_ROUTE(4, "偏离路线"),
    BINDING_TIMEOUT(5, "绑定超时未交车"),
    VEHICLE_OFF_HOURS(6, "非工作时段用车");

    private final int value;
    private final String description;

    public static AlertTypeEnum fromValue(int value) {
        for (AlertTypeEnum a : values()) {
            if (a.value == value) return a;
        }
        return POSITION_MISMATCH;
    }
}
