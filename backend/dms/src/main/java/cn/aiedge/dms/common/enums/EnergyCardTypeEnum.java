package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 补能卡 / 套餐类型枚举
 */
@Getter
@AllArgsConstructor
public enum EnergyCardTypeEnum {

    FUEL_CARD(1, "油卡"),
    POWER_CARD(2, "电卡"),
    SWAP_PACKAGE(3, "换电套餐"),
    CHARGE_PACKAGE(4, "充电套餐"),
    GAS_CARD(5, "加气卡");

    private final int value;
    private final String description;

    public static EnergyCardTypeEnum fromValue(Integer value) {
        if (value != null) {
            for (EnergyCardTypeEnum e : values()) {
                if (e.value == value) return e;
            }
        }
        return FUEL_CARD;
    }

    public static String textOf(Integer value) {
        return value == null ? null : fromValue(value).getDescription();
    }
}
