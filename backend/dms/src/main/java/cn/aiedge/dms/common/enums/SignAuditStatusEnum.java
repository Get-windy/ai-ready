package cn.aiedge.dms.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 签收审核状态枚举（《签收管理开发文档》§3.4 审核流转）
 *
 * <pre>
 *   提交签收 → 待审核(0) ──通过──> 已通过(1) ──> 任务置「已完成」+ 触发结算/代收货款
 *                       └──驳回──> 已驳回(2) ──> 任务退回「配送中」，通知配送员重新签收
 * </pre>
 *
 * @author AI-Ready Team
 */
@Getter
@AllArgsConstructor
public enum SignAuditStatusEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "已通过"),
    REJECTED(2, "已驳回");

    private final int value;
    private final String description;

    public static SignAuditStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (SignAuditStatusEnum e : values()) {
            if (e.value == value) {
                return e;
            }
        }
        return null;
    }

    /** 是否可流转到目标状态（待审核 → 通过/驳回；已终态不可再变更） */
    public boolean canTransitionTo(SignAuditStatusEnum target) {
        return this == PENDING && (target == APPROVED || target == REJECTED);
    }
}
