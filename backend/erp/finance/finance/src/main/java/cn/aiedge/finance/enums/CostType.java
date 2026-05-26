package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CostType {
    
    DIRECT_MATERIAL(1, "直接材料"),
    DIRECT_LABOR(2, "直接人工"),
    MANUFACTURING(3, "制造费用"),
    INDIRECT_MATERIAL(4, "间接材料"),
    INDIRECT_LABOR(5, "间接人工"),
    OTHER(6, "其他成本");
    
    private final Integer code;
    private final String name;
    
    public static CostType fromCode(Integer code) {
        for (CostType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}