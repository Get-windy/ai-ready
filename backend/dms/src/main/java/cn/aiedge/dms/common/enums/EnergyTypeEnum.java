package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 补能类型枚举
 *
 * <p>覆盖两类主体的四种补能方式：燃油车（汽油/柴油/加气）与电动车（充电/换电）。</p>
 */
@Getter
@AllArgsConstructor
public enum EnergyTypeEnum {

    GASOLINE(1, "汽油"),
    DIESEL(2, "柴油"),
    CHARGING(3, "充电"),
    BATTERY_SWAP(4, "换电"),
    GAS(5, "加气");

    private final int value;
    private final String description;

    public static EnergyTypeEnum fromValue(Integer value) {
        if (value != null) {
            for (EnergyTypeEnum e : values()) {
                if (e.value == value) return e;
            }
        }
        return GASOLINE;
    }

    public static String textOf(Integer value) {
        return value == null ? null : fromValue(value).getDescription();
    }

    /** 是否为燃油/燃气类（用于百公里油耗判定） */
    public static boolean isFuel(Integer value) {
        return value != null && (value == GASOLINE.value || value == DIESEL.value || value == GAS.value);
    }
}
