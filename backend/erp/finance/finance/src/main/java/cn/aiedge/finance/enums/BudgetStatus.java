package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BudgetStatus {
    
    DRAFT(0, "草稿"),
    SUBMITTED(1, "待审批"),
    APPROVED(2, "已审批"),
    REJECTED(3, "已驳回"),
    EXECUTING(4, "执行中"),
    COMPLETED(5, "已完成"),
    CLOSED(6, "已关闭");
    
    private final Integer code;
    private final String name;
    
    public static BudgetStatus fromCode(Integer code) {
        for (BudgetStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}