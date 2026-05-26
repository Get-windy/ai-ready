package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BankStatementStatus {
    
    UNMATCHED(0, "未对账"),
    MATCHED(1, "已对账"),
    MANUAL_MATCHED(2, "手工对账"),
    AUTO_MATCHED(3, "自动对账");
    
    private final Integer code;
    private final String name;
    
    public static BankStatementStatus fromCode(Integer code) {
        for (BankStatementStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}