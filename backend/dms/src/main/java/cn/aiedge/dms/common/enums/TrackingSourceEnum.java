package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 轨迹来源枚举（《配送跟踪开发文档》§3.2 来源 / §3.5.3「来源可溯」）
 *
 * <p>两端（司机端上报、管理端台账）共用同一套取值，用于判断数据可信度与对账。</p>
 *
 * @author AI-Ready Team
 */
@Getter
@AllArgsConstructor
public enum TrackingSourceEnum {

    APP(1, "APP上报"),
    BACKEND(2, "后台补录"),
    CHANNEL(3, "渠道回传");

    private final int value;
    private final String description;

    public static TrackingSourceEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (TrackingSourceEnum e : values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }

    /** 未知来源的展示兜底（历史数据可能为 null） */
    public static String textOf(Integer value) {
        TrackingSourceEnum e = fromValue(value);
        return e == null ? "未知来源" : e.getDescription();
    }
}
