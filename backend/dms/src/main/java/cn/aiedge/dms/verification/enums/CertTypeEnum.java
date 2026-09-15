package cn.aiedge.dms.verification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 骑手证照类型枚举
 */
@Getter
@AllArgsConstructor
public enum CertTypeEnum {

    DRIVING_LICENSE(1, "驾驶证"),
    VEHICLE_LICENSE(2, "行驶证"),
    HEALTH_CERTIFICATE(3, "健康证"),
    QUALIFICATION(4, "从业资格证"),
    OTHER(5, "其他");

    private final int value;
    private final String description;

    public static CertTypeEnum fromValue(Integer value) {
        if (value != null) {
            for (CertTypeEnum t : values()) {
                if (t.value == value) return t;
            }
        }
        return OTHER;
    }

    public static String text(Integer value) {
        return fromValue(value).getDescription();
    }
}
