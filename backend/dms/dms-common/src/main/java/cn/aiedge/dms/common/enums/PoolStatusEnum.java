package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订单大厅状态枚举
 */
@Getter
@AllArgsConstructor
public enum PoolStatusEnum {

    PENDING_GRAB(0, "待抢单"),
    BIDDING(1, "竞价中"),
    ACCEPTED(2, "已接单"),
    EXPIRED(3, "已过期"),
    REMOVED(4, "已下架");

    private final int value;
    private final String description;

    public static PoolStatusEnum fromValue(int value) {
        for (PoolStatusEnum status : values()) {
            if (status.value == value) return status;
        }
        return PENDING_GRAB;
    }
}
