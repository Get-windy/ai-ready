package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CostAllocationMethod {
    
    DIRECT(1, "直接分配法"),
    STEP(2, "顺序分配法"),
    RECURSIVE(3, "交互分配法"),
    ACTIVITY(4, "作业成本法"),
    STANDARD(5, "标准成本法");
    
    private final Integer code;
    private final String name;
    
    public static CostAllocationMethod fromCode(Integer code) {
        for (CostAllocationMethod method : values()) {
            if (method.getCode().equals(code)) {
                return method;
            }
        }
        return null;
    }
}