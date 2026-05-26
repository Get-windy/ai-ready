package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountDirection {
    
    DEBIT(1, "借方"),
    CREDIT(2, "贷方");
    
    private final Integer code;
    private final String name;
    
    public static AccountDirection fromCode(Integer code) {
        for (AccountDirection direction : values()) {
            if (direction.getCode().equals(code)) {
                return direction;
            }
        }
        return null;
    }
}