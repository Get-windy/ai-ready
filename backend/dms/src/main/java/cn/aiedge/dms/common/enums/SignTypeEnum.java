package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 签收类型枚举（《签收管理开发文档》§3.6.3 / §3.6.8 双端同一套枚举）
 *
 * <p>⚠️ 与司机端（`frontend/apps/driver-delivery`）提交的 `signType` 必须保持同一套取值，
 * 管理端只做展示与审核，禁止两端各自定义。</p>
 *
 * @author AI-Ready Team
 */
@Getter
@AllArgsConstructor
public enum SignTypeEnum {

    NORMAL(1, "正常签收"),
    PARTIAL(2, "部分签收"),
    REJECT(3, "拒收");

    private final int value;
    private final String description;

    public static SignTypeEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (SignTypeEnum e : values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }
}
