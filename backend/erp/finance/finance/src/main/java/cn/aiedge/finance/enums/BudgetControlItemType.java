package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BudgetControlItemType {
    
    SUBJECT(1, "科目控制"),
    DEPARTMENT(2, "部门控制"),
    PROJECT(3, "项目控制"),
    COMBINED(4, "组合控制");
    
    private final Integer code;
    private final String name;
    
    public static BudgetControlItemType fromCode(Integer code) {
        for (BudgetControlItemType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}