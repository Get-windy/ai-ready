package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 车辆维保类型枚举（全系统唯一口径：前端下拉 / 列表标签 / Excel 导出共用）
 *
 * <p>2026-09-12 金标准修复：原前端仅 1–5、后端实体注释含 6-其他，两侧不一致；
 * 统一以本枚举为准（含 6 其他）。</p>
 */
@Getter
@AllArgsConstructor
public enum MaintTypeEnum {

    UPKEEP(1, "保养"),
    REPAIR(2, "维修"),
    ANNUAL_INSPECTION(3, "年检"),
    INSURANCE(4, "保险"),
    ACCIDENT(5, "事故"),
    OTHER(6, "其他");

    private final int value;
    private final String text;

    public static String textOf(Integer value) {
        if (value == null) {
            return "";
        }
        for (MaintTypeEnum item : values()) {
            if (item.value == value) {
                return item.text;
            }
        }
        return String.valueOf(value);
    }

    /** 是否为周期性保养（需推算下次保养里程） */
    public static boolean isUpkeep(Integer value) {
        return value != null && value == UPKEEP.value;
    }

    /** 是否为证件类（年检/保险，需回写车辆到期日） */
    public static boolean isCertificate(Integer value) {
        return value != null && (value == ANNUAL_INSPECTION.value || value == INSURANCE.value);
    }
}
