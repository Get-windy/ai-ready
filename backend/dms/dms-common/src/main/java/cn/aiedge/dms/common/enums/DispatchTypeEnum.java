package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分配方式枚举
 */
@Getter
@AllArgsConstructor
public enum DispatchTypeEnum {

    AUTO(1, "自动分配"),
    MANUAL(2, "手动指派"),
    GRAB(3, "抢单"),
    BID(4, "竞价");

    private final int value;
    private final String description;

    public static DispatchTypeEnum fromValue(int value) {
        for (DispatchTypeEnum type : values()) {
            if (type.value == value) return type;
        }
        return AUTO;
    }
}
