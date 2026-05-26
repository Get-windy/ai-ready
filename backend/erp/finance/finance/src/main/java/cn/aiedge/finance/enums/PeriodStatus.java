package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PeriodStatus {
    
    OPEN(0, "未开账"),
    ACTIVE(1, "已开账"),
    CLOSED(2, "已结账"),
    FROZEN(3, "已冻结");
    
    private final Integer code;
    private final String name;
    
    public static PeriodStatus fromCode(Integer code) {
        for (PeriodStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}