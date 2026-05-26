package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuxiliaryType {
    
    DEPARTMENT(1, "部门核算"),
    PROJECT(2, "项目核算"),
    CUSTOMER(3, "客户核算"),
    SUPPLIER(4, "供应商核算"),
    PERSONNEL(5, "个人核算"),
    INVENTORY(6, "存货核算"),
    CASH_FLOW(7, "现金流量"),
    CUSTOM(8, "自定义核算");
    
    private final Integer code;
    private final String name;
    
    public static AuxiliaryType fromCode(Integer code) {
        for (AuxiliaryType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}