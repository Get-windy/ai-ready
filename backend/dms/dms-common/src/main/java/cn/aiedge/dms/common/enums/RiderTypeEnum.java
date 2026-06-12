package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 配送员类型枚举
 */
@Getter
@AllArgsConstructor
public enum RiderTypeEnum {

    OWN_STAFF(1, "自有员工"),
    CROWD_SOURCE(2, "众包兼职"),
    PLATFORM_RIDER(3, "外部平台骑手"),
    SOCIAL_DRIVER(4, "社会车辆司机");

    private final int value;
    private final String description;

    public static RiderTypeEnum fromValue(int value) {
        for (RiderTypeEnum type : values()) {
            if (type.value == value) return type;
        }
        return OWN_STAFF;
    }
}
