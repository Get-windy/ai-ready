package cn.aiedge.finance.enums;

public enum ReceivableStatus {
    PENDING(0, "待收款"),
    PARTIAL(1, "部分收款"),
    RECEIVED(2, "已收款"),
    OVERDUE(3, "逾期"),
    CLOSED(4, "已关闭"),
    CANCELLED(5, "已取消");

    private final Integer code;
    private final String desc;

    ReceivableStatus(Integer code, String desc) {
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