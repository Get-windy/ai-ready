package cn.aiedge.dms.verification.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 实名认证（KYC）状态枚举
 *
 * <p>流转：待提交 → 待审核 → 已通过 / 已驳回；已通过后证照或背书过期自动置为已过期。</p>
 */
@Getter
@AllArgsConstructor
public enum VerifyStatusEnum {

    DRAFT(0, "待提交"),
    PENDING(1, "待审核"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已驳回"),
    EXPIRED(4, "已过期");

    private final int value;
    private final String description;

    public static VerifyStatusEnum fromValue(Integer value) {
        if (value != null) {
            for (VerifyStatusEnum s : values()) {
                if (s.value == value) return s;
            }
        }
        return DRAFT;
    }

    /** 是否允许接单（唯一放行口径：已通过） */
    public static boolean eligible(Integer value) {
        return value != null && value == APPROVED.value;
    }
}
