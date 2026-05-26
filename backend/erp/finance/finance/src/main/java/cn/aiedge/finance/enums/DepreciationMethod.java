package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DepreciationMethod {
    
    STRAIGHT_LINE(1, "年限平均法"),
    WORKLOAD(2, "工作量法"),
    DOUBLE_DECLINING(3, "双倍余额递减法"),
    SUM_OF_YEARS(4, "年数总和法"),
    NONE(5, "不提折旧");
    
    private final Integer code;
    private final String name;
    
    public static DepreciationMethod fromCode(Integer code) {
        for (DepreciationMethod method : values()) {
            if (method.getCode().equals(code)) {
                return method;
            }
        }
        return null;
    }
}