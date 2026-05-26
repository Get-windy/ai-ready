package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BudgetType {
    
    ANNUAL(1, "年度预算"),
    QUARTERLY(2, "季度预算"),
    MONTHLY(3, "月度预算"),
    PROJECT(4, "项目预算"),
    DEPARTMENT(5, "部门预算");
    
    private final Integer code;
    private final String name;
    
    public static BudgetType fromCode(Integer code) {
        for (BudgetType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}