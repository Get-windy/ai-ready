package cn.aiedge.erp.batchsn.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 质量状态枚举
 *
 * @author team-member
 * @date 2026-04-29
 */
@Getter
@AllArgsConstructor
public enum QualityStatusEnum {

    NORMAL("NORMAL", "正常"),
    QUARANTINED("QUARANTINED", "待检"),
    DEFECTIVE("DEFECTIVE", "不合格"),
    UNDER_REPAIR("UNDER_REPAIR", "维修中");

    private final String code;
    private final String description;

    public static QualityStatusEnum fromCode(String code) {
        for (QualityStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown quality status code: " + code);
    }
}
