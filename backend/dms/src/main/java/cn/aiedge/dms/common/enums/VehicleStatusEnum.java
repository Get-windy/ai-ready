package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 车辆状态枚举（《车辆管理开发文档》§3.2 状态机固化）
 *
 * <pre>
 *   空闲(0) ⇄ 使用中(1) ； 使用中(1) → 维修中(2) → 空闲(0) ； 任意 → 已报废(3)（终态）
 *   已出勤(4)：出车但未接单（与「使用中(1)＝有在途任务」区分）
 * </pre>
 *
 * @author AI-Ready Team
 */
@Getter
@AllArgsConstructor
public enum VehicleStatusEnum {

    IDLE(0, "空闲"),
    IN_USE(1, "使用中"),
    REPAIRING(2, "维修中"),
    SCRAPPED(3, "已报废"),
    ON_DUTY(4, "已出勤");

    private final int value;
    private final String description;

    public static VehicleStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (VehicleStatusEnum s : values()) {
            if (s.value == value) {
                return s;
            }
        }
        return null;
    }

    /** 是否可流转到目标状态（已报废为终态，不可再变更） */
    public boolean canTransitionTo(VehicleStatusEnum target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            // 空闲 / 使用中 / 已出勤：可流转到其余任一状态（含直接报废）
            case IDLE, IN_USE, ON_DUTY -> true;
            // 维修中：只能回到空闲，或报废
            case REPAIRING -> target == IDLE || target == SCRAPPED;
            // 已报废：终态
            case SCRAPPED -> false;
        };
    }
}
