package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AccountType {
    
    ASSET(1, "资产类"),
    LIABILITY(2, "负债类"),
    EQUITY(3, "所有者权益类"),
    COST(4, "成本类"),
    PROFIT_LOSS(5, "损益类");
    
    private final Integer code;
    private final String name;
    
    public static AccountType fromCode(Integer code) {
        for (AccountType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}