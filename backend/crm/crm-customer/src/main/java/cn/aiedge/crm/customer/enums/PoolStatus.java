package cn.aiedge.crm.customer.enums;

public enum PoolStatus {
    AVAILABLE(1, "可领取"),
    CLAIMED(2, "已领取"),
    EXPIRED(3, "已过期"),
    RETURNED(4, "已退回");

    private final Integer code;
    private final String desc;

    PoolStatus(Integer code, String desc) {
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