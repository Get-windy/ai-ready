package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 配送任务状态枚举
 *
 * 状态机流转：
 * 0-待分配 -> 1-已分配 -> 2-已接单 -> 3-取货中 -> 4-配送中 -> 5-已签收 -> 6-已完成
 *    |           |           |           |            |
 *    +-> 7-取消   +-> 7-取消  +-> 8-异常  +-> 8-异常   +-> 8-异常
 */
@Getter
@AllArgsConstructor
public enum TaskStatusEnum {

    PENDING(0, "待分配"),
    ASSIGNED(1, "已分配"),
    ACCEPTED(2, "已接单"),
    PICKING_UP(3, "取货中"),
    DELIVERING(4, "配送中"),
    SIGNED(5, "已签收"),
    COMPLETED(6, "已完成"),
    CANCELLED(7, "已取消"),
    EXCEPTION(8, "异常");

    private final int value;
    private final String description;

    public static TaskStatusEnum fromValue(int value) {
        for (TaskStatusEnum status : values()) {
            if (status.value == value) return status;
        }
        return PENDING;
    }

    /**
     * 判断是否可流转到目标状态
     */
    public boolean canTransitionTo(TaskStatusEnum target) {
        switch (this) {
            case PENDING:
                return target == ASSIGNED || target == CANCELLED;
            case ASSIGNED:
                return target == ACCEPTED || target == CANCELLED;
            case ACCEPTED:
                return target == PICKING_UP || target == EXCEPTION;
            case PICKING_UP:
                return target == DELIVERING || target == EXCEPTION;
            case DELIVERING:
                return target == SIGNED || target == EXCEPTION;
            case SIGNED:
                return target == COMPLETED || target == EXCEPTION;
            case COMPLETED:
            case CANCELLED:
                return false; // 终态
            case EXCEPTION:
                return target == PICKING_UP || target == DELIVERING; // 异常可恢复
            default:
                return false;
        }
    }
}
