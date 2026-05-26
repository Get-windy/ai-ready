package cn.aiedge.crm.customer.enums;

public enum PoolType {
    PUBLIC(1, "公海池"),
    RECOVERY(2, "回收池"),
    ASSIGN(3, "分配池");

    private final Integer code;
    private final String desc;

    PoolType(Integer code, String desc) {
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