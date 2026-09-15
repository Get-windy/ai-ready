package cn.aiedge.dms.verification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 证照核验状态枚举
 */
@Getter
@AllArgsConstructor
public enum CertStatusEnum {

    UNVERIFIED(0, "待核验"),
    VALID(1, "有效"),
    EXPIRED(2, "已过期"),
    INVALID(3, "无效");

    private final int value;
    private final String description;

    public static CertStatusEnum fromValue(Integer value) {
        if (value != null) {
            for (CertStatusEnum s : values()) {
                if (s.value == value) return s;
            }
        }
        return UNVERIFIED;
    }

    public static String text(Integer value) {
        return fromValue(value).getDescription();
    }
}
