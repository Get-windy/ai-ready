package cn.aiedge.finance.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum VoucherStatus {
    
    DRAFT(0, "草稿"),
    SUBMITTED(1, "待审核"),
    APPROVED(2, "已审核"),
    POSTED(3, "已记账"),
    REJECTED(4, "已驳回"),
    VOIDED(5, "已作废");
    
    private final Integer code;
    private final String name;
    
    public static VoucherStatus fromCode(Integer code) {
        for (VoucherStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}