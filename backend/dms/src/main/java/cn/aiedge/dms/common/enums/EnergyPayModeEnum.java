package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 补能支付方式枚举
 *
 * <p>{@code MONTHLY_PACKAGE} 用于骑手两轮换电的月租制（如「225 元/月无限次换电」），
 * 此类记录通常按当班里程分摊为每公里成本。</p>
 */
@Getter
@AllArgsConstructor
public enum EnergyPayModeEnum {

    CASH(1, "现金"),
    FUEL_CARD(2, "油卡"),
    POWER_CARD(3, "电卡"),
    MONTHLY_PACKAGE(4, "月租套餐"),
    PLATFORM(5, "平台代扣");

    private final int value;
    private final String description;

    public static EnergyPayModeEnum fromValue(Integer value) {
        if (value != null) {
            for (EnergyPayModeEnum e : values()) {
                if (e.value == value) return e;
            }
        }
        return CASH;
    }

    public static String textOf(Integer value) {
        return value == null ? null : fromValue(value).getDescription();
    }
}
