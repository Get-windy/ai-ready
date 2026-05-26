package cn.aiedge.erp.payment.enums;

public enum PaymentMethod {
    CASH(1, "现金"),
    BANK_TRANSFER(2, "银行转账"),
    CHECK(3, "支票"),
    CREDIT_CARD(4, "信用卡"),
    ONLINE(5, "在线支付"),
    OFFSET(6, "抵扣"),
    OTHER(7, "其他");

    private final Integer code;
    private final String desc;

    PaymentMethod(Integer code, String desc) {
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