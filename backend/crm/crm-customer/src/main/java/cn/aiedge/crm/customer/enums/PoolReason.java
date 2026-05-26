package cn.aiedge.crm.customer.enums;

public enum PoolReason {
    NO_FOLLOW_UP(1, "长期未跟进"),
    SALES_PERSON_LEAVE(2, "销售人员离职"),
    CUSTOMER_REQUEST(3, "客户要求"),
    MANUAL_PUT(4, "手动放入"),
    SYSTEM_AUTO(5, "系统自动");

    private final Integer code;
    private final String desc;

    PoolReason(Integer code, String desc) {
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