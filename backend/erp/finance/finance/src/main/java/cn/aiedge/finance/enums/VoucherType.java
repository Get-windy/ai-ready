package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VoucherType {
    
    RECEIPT(1, "收款凭证"),
    PAYMENT(2, "付款凭证"),
    TRANSFER(3, "转账凭证"),
    GENERAL(4, "通用凭证");
    
    private final Integer code;
    private final String name;
    
    public static VoucherType fromCode(Integer code) {
        for (VoucherType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}