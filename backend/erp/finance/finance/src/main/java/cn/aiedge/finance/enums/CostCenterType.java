package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CostCenterType {
    
    DEPARTMENT(1, "部门成本中心"),
    PROJECT(2, "项目成本中心"),
    PRODUCT(3, "产品成本中心"),
    PROCESS(4, "工序成本中心"),
    ACTIVITY(5, "作业成本中心");
    
    private final Integer code;
    private final String name;
    
    public static CostCenterType fromCode(Integer code) {
        for (CostCenterType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}