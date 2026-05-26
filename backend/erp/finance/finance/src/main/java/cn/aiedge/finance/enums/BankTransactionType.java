package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BankTransactionType {
    
    DEPOSIT(1, "存入"),
    WITHDRAW(2, "取出"),
    TRANSFER_IN(3, "转入"),
    TRANSFER_OUT(4, "转出"),
    INTEREST(5, "利息"),
    FEE(6, "手续费"),
    OTHER(7, "其他");
    
    private final Integer code;
    private final String name;
    
    public static BankTransactionType fromCode(Integer code) {
        for (BankTransactionType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}