package cn.aiedge.erp.sale.return.enums;

public enum RefundMethod {
    ORIGINAL(1, "原路退回"),
    BANK_TRANSFER(2, "银行转账"),
    CASH(3, "现金退款"),
    OFFSET(4, "抵扣欠款"),
    CREDIT(5, "转为预存款");

    private final Integer code;
    private final String desc;

    RefundMethod(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}