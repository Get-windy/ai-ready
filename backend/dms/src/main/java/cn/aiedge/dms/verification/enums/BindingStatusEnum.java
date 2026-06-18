package cn.aiedge.dms.verification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 人车绑定状态枚举
 */
@Getter
@AllArgsConstructor
public enum BindingStatusEnum {

    ACTIVE(0, "绑定中（配送中）"),
    HANDED_OVER(1, "已交车"),
    ABNORMAL(2, "异常解绑");

    private final int value;
    private final String description;

    public static BindingStatusEnum fromValue(int value) {
        for (BindingStatusEnum s : values()) {
            if (s.value == value) return s;
        }
        return ACTIVE;
    }
}
