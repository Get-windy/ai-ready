package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 配送渠道类型枚举
 */
@Getter
@AllArgsConstructor
public enum ChannelTypeEnum {

    OWN_STAFF(1, "自有员工"),
    CROWD_SOURCE(2, "众包兼职"),
    EXTERNAL_PLATFORM(3, "外部平台"),
    SOCIAL_VEHICLE(4, "社会车辆");

    private final int value;
    private final String description;

    public static ChannelTypeEnum fromValue(int value) {
        for (ChannelTypeEnum type : values()) {
            if (type.value == value) return type;
        }
        return OWN_STAFF;
    }
}
