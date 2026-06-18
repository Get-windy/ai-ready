package cn.aiedge.erp.batchsn.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 序列号阶段枚举
 *
 * @author team-member
 * @date 2026-04-29
 */
@Getter
@AllArgsConstructor
public enum SnStageEnum {

    WAREHOUSE("WAREHOUSE", "仓库"),
    IN_TRANSIT("IN_TRANSIT", "在途"),
    EOF_CUSTOMER("EOF_CUSTOMER", "客户处"),
    IN_SERVICE("IN_SERVICE", "服务中"),
    SCRAPPED("SCRAPPED", "已报废");

    private final String code;
    private final String description;

    public static SnStageEnum fromCode(String code) {
        for (SnStageEnum stage : values()) {
            if (stage.code.equals(code)) {
                return stage;
            }
        }
        throw new IllegalArgumentException("Unknown SN stage code: " + code);
    }
}
