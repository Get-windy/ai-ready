package cn.aiedge.crm.marketing.enums;

public enum TargetStatus {
    PENDING(0, "待触达"),
    REACHED(1, "已触达"),
    RESPONDED(2, "已响应"),
    CONVERTED(3, "已转化"),
    FAILED(4, "触达失败"),
    REJECTED(5, "已拒绝");

    private final Integer code;
    private final String desc;

    TargetStatus(Integer code, String desc) {
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