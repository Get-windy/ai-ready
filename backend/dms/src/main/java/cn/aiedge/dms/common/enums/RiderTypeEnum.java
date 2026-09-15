package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 配送员类型枚举
 *
 * <p>业务口径：配送员可以是<b>企业员工</b>（自有配送员，关联系统用户 userId），
 * 也可以是<b>外部平台</b>的骑手/配送员（众包、第三方运力，关联渠道 channelId）。</p>
 */
@Getter
@AllArgsConstructor
public enum RiderTypeEnum {

    OWN_STAFF(1, "企业员工"),
    CROWD_SOURCE(2, "众包兼职"),
    PLATFORM_RIDER(3, "外部平台配送员"),
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
