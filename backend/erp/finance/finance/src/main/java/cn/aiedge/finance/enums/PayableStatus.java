package cn.aiedge.finance.enums;

public enum PayableStatus {
    PENDING(0, "待付款"),
    PARTIAL(1, "部分付款"),
    PAID(2, "已付款"),
    OVERDUE(3, "逾期"),
    CLOSED(4, "已关闭"),
    CANCELLED(5, "已取消");

    private final Integer code;
    private final String desc;

    PayableStatus(Integer code, String desc) {
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